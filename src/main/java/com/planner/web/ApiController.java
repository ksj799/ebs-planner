package com.planner.web;

import com.planner.config.PlannerProperties;
import com.planner.model.Model.InterfaceRow;
import com.planner.model.Model.MasterData;
import com.planner.model.Model.OnhandRow;
import com.planner.model.Model.Org;
import com.planner.model.Model.PurchasingData;
import com.planner.repo.ItemRepository;
import com.planner.repo.MaterialRepository;
import org.springframework.web.bind.annotation.PathVariable;
import com.planner.repo.PlannerStore;
import com.planner.service.MaterialService;
import org.springframework.web.bind.annotation.PutMapping;
import com.planner.model.Model.PlanRequest;
import com.planner.model.Model.PlanResult;
import com.planner.model.Model.ReleaseRequest;
import com.planner.model.Model.ReleaseResult;
import com.planner.repo.EbsRepository;
import com.planner.service.PlanningService;
import com.planner.service.ReleaseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ApiController {
    private static final Logger log = LoggerFactory.getLogger(ApiController.class);
    private final EbsRepository repo;
    private final MaterialRepository materialRepo;
    private final ItemRepository itemRepo;
    private final PlanningService planning;
    private final MaterialService material;
    private final ReleaseService release;
    private final PlannerStore store;
    private final PlannerProperties props;
    private final Environment env;

    public ApiController(EbsRepository repo, MaterialRepository materialRepo, ItemRepository itemRepo,
                         PlanningService planning, MaterialService material, ReleaseService release,
                         PlannerStore store, PlannerProperties props, Environment env) {
        this.repo = repo;
        this.materialRepo = materialRepo;
        this.itemRepo = itemRepo;
        this.planning = planning;
        this.material = material;
        this.release = release;
        this.store = store;
        this.props = props;
        this.env = env;
    }

    @GetMapping("/info")
    public Map<String, Object> info() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("demo", env.acceptsProfiles(Profiles.of("demo")));
        m.put("readOnly", true); // EBS 반영 로직 주석 처리됨 (ReleaseService, WipInterfaceRepository)
        m.put("banner", props.getBanner());
        m.put("sourceCode", props.getSourceCode());
        m.put("userConfigured", props.getUserId() != null);
        m.put("submitMassLoad", props.getMassLoad().isSubmit());
        m.put("statusType", props.getWip().getStatusType());
        m.put("schedulingMethod", props.getWip().getSchedulingMethod());
        m.put("classCode", props.getWip().getClassCode());
        return m;
    }

    @GetMapping("/orgs")
    public List<Org> orgs() {
        return repo.orgs();
    }

    @PostMapping("/plan")
    public PlanResult plan(@RequestBody PlanRequest req) {
        return planning.plan(req);
    }

    @PostMapping("/release")
    public ReleaseResult release(@RequestBody ReleaseRequest req) {
        ReleaseResult r = release.release(req);
        log.info("작업지시 인터페이스 적재: org={} group={} count={} request={}", req.orgId, r.groupId, r.count, r.requestId);
        return r;
    }

    @GetMapping("/interface")
    public List<InterfaceRow> interfaceRows(@RequestParam long orgId, @RequestParam(defaultValue = "14") int days) {
        return repo.interfaceRows(orgId, props.getSourceCode(), LocalDate.now().minusDays(days));
    }

    @GetMapping("/master")
    public MasterData master(@RequestParam long orgId) {
        return planning.master(orgId);
    }

    // ---------- 품목 ----------

    @GetMapping("/items")
    public List<Map<String, Object>> items(@RequestParam long orgId, @RequestParam(required = false) String q,
                                           @RequestParam(required = false) Integer makeBuy,
                                           @RequestParam(defaultValue = "300") int limit) {
        return itemRepo.search(orgId, q, makeBuy, Math.max(1, Math.min(limit, 1000)));
    }

    @GetMapping("/items/{itemId}")
    public Map<String, Object> item(@RequestParam long orgId, @PathVariable long itemId) {
        LocalDate today = LocalDate.now();
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("attrs", itemRepo.detail(orgId, itemId));
        m.put("supply", itemRepo.supply(orgId, itemId));
        m.put("bom", itemRepo.bom(orgId, itemId, today));
        m.put("whereUsed", itemRepo.whereUsed(orgId, itemId, today));
        m.put("leadTimeOverride", store.leadTimes(orgId).get(itemId));
        return m;
    }

    // ---------- 재고 / 구매 ----------

    @GetMapping("/onhand")
    public List<OnhandRow> onhand(@RequestParam long orgId) {
        return materialRepo.onhandDetail(orgId);
    }

    @GetMapping("/purchasing")
    public PurchasingData purchasing(@RequestParam long orgId) {
        return material.purchasing(orgId);
    }

    public static class ReceiptBody {
        public long orgId;
        public long lineLocationId;
        public String date;
        public String note;
    }

    @PutMapping("/purchasing/receipt")
    public Map<String, Object> setReceipt(@RequestBody ReceiptBody b) {
        PlannerStore.ReceiptOverride o = null;
        if (b.date != null && !b.date.isEmpty()) {
            LocalDate.parse(b.date); // 형식 검증
            o = new PlannerStore.ReceiptOverride();
            o.date = b.date;
            o.note = b.note;
        }
        store.setReceipt(b.orgId, b.lineLocationId, o);
        return Collections.<String, Object>singletonMap("ok", true);
    }

    public static class LeadTimeBody {
        public long orgId;
        public long itemId;
        public Double pre;
        public Double full;
        public Double post;
    }

    @PutMapping("/purchasing/leadtime")
    public Map<String, Object> setLeadTime(@RequestBody LeadTimeBody b) {
        for (Double v : new Double[]{b.pre, b.full, b.post}) {
            if (v != null && (v < 0 || v > 999)) throw new IllegalArgumentException("리드타임은 0~999일 사이여야 합니다.");
        }
        PlannerStore.LeadTimeOverride o = new PlannerStore.LeadTimeOverride();
        o.pre = b.pre;
        o.full = b.full;
        o.post = b.post;
        store.setLeadTime(b.orgId, b.itemId, o);
        return Collections.<String, Object>singletonMap("ok", true);
    }

    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class, java.time.format.DateTimeParseException.class})
    public ResponseEntity<Map<String, String>> badRequest(RuntimeException e) {
        return ResponseEntity.badRequest().body(Collections.singletonMap("message", e.getMessage()));
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<Map<String, String>> db(DataAccessException e) {
        log.error("DB 오류", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Collections.singletonMap("message", "DB 오류: " + e.getMostSpecificCause().getMessage()));
    }
}
