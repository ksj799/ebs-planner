package com.planner.service;

import com.planner.model.Model.Adjust;
import com.planner.model.Model.BomComponent;
import com.planner.model.Model.ItemPlanning;
import com.planner.model.Model.MaterialRow;
import com.planner.model.Model.Org;
import com.planner.model.Model.PlannedJob;
import com.planner.model.Model.PoShipment;
import com.planner.model.Model.PurchaseSuggestion;
import com.planner.model.Model.PurchasingData;
import com.planner.model.Model.WipRequirement;
import com.planner.repo.EbsRepository;
import com.planner.repo.MaterialRepository;
import com.planner.repo.PlannerStore;
import com.planner.repo.PlannerStore.LeadTimeOverride;
import com.planner.repo.PlannerStore.ReceiptOverride;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeSet;

/**
 * 구매부품 수급.
 * 공급 = 현재고(Nettable) + 미입고 PO(입고예정일 + 후처리 LT 후 가용).
 * 소요 = 기존 작업지시 미출고분 → 계획 작업지시(납기순, 1단계 BOM) 순으로 선입선출 배정.
 * 모자라면 리드타임을 반영한 구매 제안을 만들고, 그 가용일까지 작업지시 시작을 늦춘다.
 */
@Service
public class MaterialService {
    private static final double EPS = 1e-9;
    private final EbsRepository ebs;
    private final MaterialRepository repo;
    private final PlannerStore store;

    public MaterialService(EbsRepository ebs, MaterialRepository repo, PlannerStore store) {
        this.ebs = ebs;
        this.repo = repo;
        this.store = store;
    }

    public static class Result {
        public List<MaterialRow> rows = new ArrayList<>();
        public List<PurchaseSuggestion> purchases = new ArrayList<>();
    }

    private static class Lot {
        double qty;
        final LocalDate avail;
        Lot(double qty, LocalDate avail) { this.qty = qty; this.avail = avail; }
    }

    private static class State {
        ItemPlanning ip;
        MaterialRow row = new MaterialRow();
        List<Lot> lots = new ArrayList<>();
    }

    // ---------- 근무일 계산 ----------

    static LocalDate addWork(NavigableSet<LocalDate> work, LocalDate from, int n) {
        LocalDate d = work.ceiling(from);
        if (d == null) return from.plusDays(n);
        for (int i = 0; i < n; i++) {
            LocalDate x = work.higher(d);
            if (x == null) return d.plusDays(n - i);
            d = x;
        }
        return d;
    }

    static LocalDate subWork(NavigableSet<LocalDate> work, LocalDate from, int n) {
        LocalDate d = work.floor(from);
        if (d == null) return from.minusDays(n);
        for (int i = 0; i < n; i++) {
            LocalDate x = work.lower(d);
            if (x == null) return d.minusDays(n - i);
            d = x;
        }
        return d;
    }

    private static int days(double lt) {
        return (int) Math.ceil(lt - EPS);
    }

    private static LocalDate max(LocalDate a, LocalDate b) {
        if (a == null) return b;
        if (b == null) return a;
        return a.isAfter(b) ? a : b;
    }

    // ---------- 기준정보 ----------

    /** EBS 리드타임에 플래너 수정값을 덮어쓴 품목 계획 속성 */
    public Map<Long, ItemPlanning> itemPlanning(long orgId, Collection<Long> ids) {
        if (ids.isEmpty()) return new LinkedHashMap<>();
        Map<Long, ItemPlanning> items = repo.itemPlanning(orgId, ids);
        Map<Long, LeadTimeOverride> ov = store.leadTimes(orgId);
        for (ItemPlanning it : items.values()) {
            LeadTimeOverride o = ov.get(it.itemId);
            it.pre = o != null && o.pre != null ? o.pre : it.ebsPre;
            it.full = o != null && o.full != null ? o.full : it.ebsFull;
            it.post = o != null && o.post != null ? o.post : it.ebsPost;
            it.overridden = o != null;
            it.totalLt = days(it.pre) + days(it.full) + days(it.post);
        }
        return items;
    }

    private List<PoShipment> shipments(long orgId, NavigableSet<LocalDate> work, Map<Long, ItemPlanning> items, LocalDate today) {
        List<PoShipment> pos = repo.openPo(orgId);
        Map<Long, ReceiptOverride> ov = store.receipts(orgId);
        for (PoShipment s : pos) {
            ReceiptOverride o = ov.get(s.lineLocationId);
            if (o != null) {
                s.overrideDate = LocalDate.parse(o.date);
                s.overrideNote = o.note;
            }
            if (s.overrideDate != null) { s.expected = s.overrideDate; s.dateSource = "수정"; }
            else if (s.promised != null) { s.expected = s.promised; s.dateSource = "약속일"; }
            else if (s.needBy != null) { s.expected = s.needBy; s.dateSource = "필요일"; }
            else { s.expected = today; s.dateSource = "날짜없음"; }
            s.pastDue = s.expected.isBefore(today);
            LocalDate base = s.pastDue ? today : s.expected;
            ItemPlanning ip = items.get(s.itemId);
            int post = ip == null ? 0 : days(ip.post);
            s.available = post > 0 ? addWork(work, base, post) : base;
        }
        return pos;
    }

    public PurchasingData purchasing(long orgId) {
        PurchasingData d = new PurchasingData();
        d.org = ebs.org(orgId);
        LocalDate today = LocalDate.now();
        TreeSet<LocalDate> work = new TreeSet<>(ebs.workdays(d.org, today.minusDays(30), today.plusDays(730)));
        Set<Long> ids = new LinkedHashSet<>();
        for (BomComponent c : repo.bomComponents(orgId, today)) ids.add(c.componentId);
        List<PoShipment> raw = repo.openPo(orgId);
        for (PoShipment s : raw) ids.add(s.itemId);
        Map<Long, ItemPlanning> items = itemPlanning(orgId, ids);
        d.shipments = shipments(orgId, work, items, today);
        for (ItemPlanning it : items.values()) {
            if (it.makeBuy == 2) d.items.add(it);
        }
        d.items.sort(Comparator.comparing(it -> it.itemCode));
        return d;
    }

    // ---------- 자재 계획 ----------

    /** workDays: 계획 작업지시별 생산 소요 근무일 (자재 필요일을 납기에서 거꾸로 잡을 때 사용) */
    public Result plan(Org org, LocalDate start, NavigableSet<LocalDate> work, List<PlannedJob> planned, Map<String, Adjust> adj,
                       Map<String, Integer> workDays) {
        Result result = new Result();
        long orgId = org.organizationId;
        LocalDate today = LocalDate.now();

        Map<Long, List<BomComponent>> bom = new HashMap<>();
        for (BomComponent c : repo.bomComponents(orgId, today)) {
            bom.computeIfAbsent(c.assemblyId, k -> new ArrayList<>()).add(c);
        }
        Set<Long> compIds = new LinkedHashSet<>();
        for (PlannedJob p : planned) {
            List<BomComponent> cs = bom.get(p.itemId);
            if (cs != null) for (BomComponent c : cs) compIds.add(c.componentId);
        }
        if (compIds.isEmpty()) return result;

        Map<Long, ItemPlanning> items = itemPlanning(orgId, compIds);
        LocalDate availNow = work.ceiling(today) != null ? work.ceiling(today) : today;
        Map<Long, Double> onhand = ebs.onhand(orgId);
        Map<Long, State> st = new LinkedHashMap<>();
        for (ItemPlanning ip : items.values()) {
            if (ip.makeBuy != 2) continue; // 제조 하위품목은 이번 범위 밖(1단계만)
            State s = new State();
            s.ip = ip;
            s.row.itemId = ip.itemId;
            s.row.itemCode = ip.itemCode;
            s.row.itemDesc = ip.itemDesc;
            s.row.uom = ip.uom;
            s.row.totalLt = ip.totalLt;
            s.row.onhand = Math.max(0, onhand.getOrDefault(ip.itemId, 0.0));
            if (s.row.onhand > EPS) s.lots.add(new Lot(s.row.onhand, availNow));
            st.put(ip.itemId, s);
        }
        for (PoShipment po : shipments(orgId, work, items, today)) {
            State s = st.get(po.itemId);
            if (s == null) continue;
            s.row.poQty += po.openQty;
            s.lots.add(new Lot(po.openQty, po.available));
        }
        for (State s : st.values()) s.lots.sort(Comparator.comparing(l -> l.avail));

        for (WipRequirement w : repo.wipRequirements(orgId)) {
            State s = st.get(w.itemId);
            if (s == null) continue;
            s.row.wipReq += w.openQty;
            LocalDate need = w.dateRequired == null || w.dateRequired.isBefore(today) ? availNow : w.dateRequired;
            allocate(s, w.openQty, need, "작업지시 " + w.jobName, work, today, result);
        }

        for (PlannedJob p : planned) {
            List<BomComponent> cs = bom.get(p.itemId);
            if (cs == null) continue;
            Adjust a = adj == null ? null : adj.get(p.id);
            boolean fixedStart = a != null && a.start != null && !a.start.isEmpty();
            LocalDate earliest = fixedStart ? LocalDate.parse(a.start) : start;
            LocalDate earliestW = work.ceiling(earliest) != null ? work.ceiling(earliest) : earliest;
            // 자재 필요일 = 납기를 지키는 가장 늦은 시작일(납기 − 생산 소요 근무일). 시작일을 직접 지정했으면 그 날.
            LocalDate need = earliestW;
            Integer dur = workDays == null ? null : workDays.get(p.id);
            if (!fixedStart && dur != null && p.due != null) {
                LocalDate back = subWork(work, p.due, dur);
                if (back.isAfter(need)) need = back;
            }
            LocalDate ready = earliestW;
            for (BomComponent c : cs) {
                State s = st.get(c.componentId);
                if (s == null) continue;
                double q = p.qty * c.qtyPer / c.yield;
                s.row.plannedReq += q;
                LocalDate d = allocate(s, q, need, "SO " + p.orderNumber + "-" + p.lineRef, work, today, result);
                if (d.isAfter(earliestW)) {
                    p.materialNotes.add(s.ip.itemCode + " " + d);
                    ready = max(ready, d);
                }
            }
            // 배정받은 자재가 실제로 다 들어오는 날 전에는 시작할 수 없다
            if (ready.isAfter(earliestW)) p.materialReady = ready;
        }

        for (State s : st.values()) {
            MaterialRow r = s.row;
            r.shortage = Math.max(0, r.wipReq + r.plannedReq - r.onhand - r.poQty);
            if (r.wipReq + r.plannedReq > EPS || r.poQty > EPS) result.rows.add(r);
        }
        result.rows.sort(Comparator.comparing((MaterialRow r) -> -r.shortage).thenComparing(r -> r.itemCode));
        result.purchases.sort(Comparator.comparing((PurchaseSuggestion p) -> p.orderBy).thenComparing(p -> p.itemCode));
        return result;
    }

    /** 선입선출 배정. 반환값 = 필요 수량이 모두 가용해지는 날 */
    private LocalDate allocate(State s, double qty, LocalDate need, String ref, NavigableSet<LocalDate> work,
                               LocalDate today, Result result) {
        double remain = qty;
        LocalDate ready = null;
        while (remain > EPS && !s.lots.isEmpty()) {
            Lot l = s.lots.get(0);
            double take = Math.min(l.qty, remain);
            l.qty -= take;
            remain -= take;
            ready = max(ready, l.avail);
            if (l.qty <= EPS) s.lots.remove(0);
        }
        if (remain > EPS) {
            ItemPlanning ip = s.ip;
            int pre = days(ip.pre), full = days(ip.full), post = days(ip.post);
            LocalDate earliestAvail = addWork(work, today, pre + full + post);
            LocalDate wanted = work.ceiling(need) != null ? work.ceiling(need) : need;
            LocalDate avail = wanted.isAfter(earliestAvail) ? wanted : earliestAvail;

            PurchaseSuggestion ps = new PurchaseSuggestion();
            ps.itemId = ip.itemId;
            ps.itemCode = ip.itemCode;
            ps.itemDesc = ip.itemDesc;
            ps.uom = ip.uom;
            ps.shortQty = remain;
            ps.qty = roundLot(remain, ip.minOrder, ip.lotMultiplier);
            ps.needDate = need;
            ps.availableDate = avail;
            ps.receiptDate = subWork(work, avail, post);
            ps.orderBy = subWork(work, ps.receiptDate, full + pre);
            ps.late = earliestAvail.isAfter(wanted);
            ps.forRef = ref;
            result.purchases.add(ps);

            s.row.suggestedQty += ps.qty;
            if (s.row.firstShortDate == null) s.row.firstShortDate = need;
            if (ps.qty - remain > EPS) s.lots.add(new Lot(ps.qty - remain, avail));
            ready = max(ready, avail);
        }
        return ready == null ? need : ready;
    }

    static double roundLot(double q, double minOrder, double multiple) {
        double r = Math.max(q, minOrder);
        if (multiple > 0) r = Math.ceil(r / multiple - EPS) * multiple;
        return r;
    }
}
