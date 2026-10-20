package com.planner.repo;

import com.planner.model.Model.BomComponent;
import com.planner.model.Model.ItemPlanning;
import com.planner.model.Model.OnhandRow;
import com.planner.model.Model.PoShipment;
import com.planner.model.Model.WipRequirement;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.planner.repo.EbsRepository.date;
import static com.planner.repo.EbsRepository.sqlDate;

/** 재고·구매·BOM 조회 (읽기 전용) */
@Repository
public class MaterialRepository {
    private final NamedParameterJdbcTemplate jdbc;
    private final SqlLoader sql;

    public MaterialRepository(NamedParameterJdbcTemplate jdbc, SqlLoader sql) {
        this.jdbc = jdbc;
        this.sql = sql;
    }

    public List<OnhandRow> onhandDetail(long orgId) {
        return jdbc.query(sql.get("onhand_detail"), new MapSqlParameterSource("orgId", orgId), (rs, i) -> {
            OnhandRow r = new OnhandRow();
            r.itemId = rs.getLong("inventory_item_id");
            r.itemCode = rs.getString("item_code");
            r.itemDesc = rs.getString("item_desc");
            r.uom = rs.getString("primary_uom_code");
            r.makeBuy = rs.getInt("planning_make_buy_code") == 1 ? "제조" : "구매";
            r.subinventory = rs.getString("subinventory_code");
            r.subinventoryDesc = rs.getString("subinventory_desc");
            r.nettable = rs.getInt("availability_type") == 1;
            r.locator = rs.getString("locator");
            r.lot = rs.getString("lot_number");
            r.revision = rs.getString("revision");
            r.qty = rs.getDouble("onhand_quantity");
            return r;
        });
    }

    public Map<Long, ItemPlanning> itemPlanning(long orgId, Collection<Long> itemIds) {
        Map<Long, ItemPlanning> out = new LinkedHashMap<>();
        List<Long> ids = new ArrayList<>(itemIds);
        for (int i = 0; i < ids.size(); i += 900) {
            MapSqlParameterSource p = new MapSqlParameterSource("orgId", orgId)
                    .addValue("ids", ids.subList(i, Math.min(ids.size(), i + 900)));
            jdbc.query(sql.get("items_planning"), p, (RowCallbackHandler) rs -> {
                ItemPlanning it = new ItemPlanning();
                it.itemId = rs.getLong("inventory_item_id");
                it.itemCode = rs.getString("item_code");
                it.itemDesc = rs.getString("item_desc");
                it.uom = rs.getString("primary_uom_code");
                it.makeBuy = rs.getInt("planning_make_buy_code");
                it.ebsPre = rs.getDouble("preprocessing_lead_time");
                it.ebsFull = rs.getDouble("full_lead_time");
                it.ebsPost = rs.getDouble("postprocessing_lead_time");
                it.minOrder = rs.getDouble("minimum_order_quantity");
                it.lotMultiplier = rs.getDouble("fixed_lot_multiplier");
                out.put(it.itemId, it);
            });
        }
        return out;
    }

    public List<BomComponent> bomComponents(long orgId, LocalDate today) {
        MapSqlParameterSource p = new MapSqlParameterSource("orgId", orgId).addValue("today", sqlDate(today));
        return jdbc.query(sql.get("bom_components"), p, (rs, i) -> {
            BomComponent c = new BomComponent();
            c.assemblyId = rs.getLong("assembly_item_id");
            c.componentId = rs.getLong("component_item_id");
            c.qtyPer = rs.getDouble("component_quantity");
            c.yield = rs.getDouble("component_yield_factor");
            if (c.yield <= 0) c.yield = 1;
            return c;
        });
    }

    private static String poStatus(String s) {
        if ("APPROVED".equals(s)) return "승인";
        if ("REQUIRES REAPPROVAL".equals(s)) return "재승인 필요";
        if ("IN PROCESS".equals(s) || "PRE-APPROVED".equals(s)) return "승인중";
        if ("REJECTED".equals(s)) return "반려";
        return "미완료";
    }

    public List<PoShipment> openPo(long orgId) {
        return jdbc.query(sql.get("open_po"), new MapSqlParameterSource("orgId", orgId), (rs, i) -> {
            PoShipment s = new PoShipment();
            s.lineLocationId = rs.getLong("line_location_id");
            s.poNumber = rs.getString("po_number");
            s.lineRef = EbsRepository.num(rs, "line_num") + "." + EbsRepository.num(rs, "shipment_num");
            s.supplier = rs.getString("vendor_name");
            s.status = poStatus(rs.getString("authorization_status"));
            s.itemId = rs.getLong("item_id");
            s.itemCode = rs.getString("item_code");
            s.itemDesc = rs.getString("item_desc");
            s.poUom = rs.getString("po_uom");
            s.primaryUom = rs.getString("primary_unit_of_measure");
            s.openQty = rs.getDouble("open_quantity");
            s.needBy = date(rs, "need_by_date");
            s.promised = date(rs, "promised_date");
            return s;
        });
    }

    public List<WipRequirement> wipRequirements(long orgId) {
        return jdbc.query(sql.get("wip_requirements"), new MapSqlParameterSource("orgId", orgId), (rs, i) -> {
            WipRequirement w = new WipRequirement();
            w.itemId = rs.getLong("inventory_item_id");
            w.jobName = rs.getString("wip_entity_name");
            w.dateRequired = date(rs, "date_required");
            w.openQty = rs.getDouble("open_quantity");
            return w;
        });
    }
}
