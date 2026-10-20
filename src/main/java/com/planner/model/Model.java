package com.planner.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** API 와 계획 엔진에서 쓰는 데이터 구조 (Jackson 은 public 필드를 그대로 직렬화) */
public final class Model {
    private Model() {}

    public static String key(long departmentId, long resourceId) {
        return departmentId + ":" + resourceId;
    }

    /** MTL_PARAMETERS + HR_ALL_ORGANIZATION_UNITS + WIP_PARAMETERS */
    public static class Org {
        public long organizationId;
        public String code;
        public String name;
        public String calendarCode;
        public long exceptionSetId;
        public String defaultClass;
    }

    /** BOM_DEPARTMENTS × BOM_DEPARTMENT_RESOURCES × BOM_RESOURCES */
    public static class Resource {
        public String key;
        public long departmentId;
        public long resourceId;
        public String departmentCode;
        public String resourceCode;
        public String description;
        public String uom;
        public double capacityUnits;
        public boolean available24h;
        /** 교대시간 × 능력단위 합계 (BOM_RESOURCE_SHIFTS/BOM_SHIFT_TIMES) */
        public double shiftUnitHours;
        public double hoursPerDay;
        /** 일자별 사용 시간 */
        public Map<String, Double> load = new TreeMap<>();
    }

    /** 라우팅 공정자원 한 줄 (BOM_OPERATION_RESOURCES) */
    public static class RoutingOp {
        public long itemId;
        public String itemCode;
        public String itemDesc;
        public String uom;
        public int operationSeq;
        public long departmentId;
        public long resourceId;
        public String resourceUom;
        public double usage;
        /** 1=Item(개당), 2=Lot(로트당=셋업) */
        public int basisType;
    }

    /** 품목별 병목 자원 요약 */
    public static class ItemRouting {
        public long itemId;
        public String itemCode;
        public String itemDesc;
        public String uom;
        public String resourceKey;
        public String resourceCode;
        public String departmentCode;
        public double hoursPerUnit;
        public double setupHours;
        public int operationCount;
        public double maxPerDay;
    }

    /** OE_ORDER_LINES_ALL 미출하 수주라인 */
    public static class Demand {
        public long lineId;
        public String orderNumber;
        public String lineRef;
        public String customer;
        public long itemId;
        public String itemCode;
        public String itemDesc;
        public String uom;
        public String orderUom;
        public double openQty;
        public LocalDate due;
        public double coveredQty;
        public double shortQty;
        public String plannedId;
    }

    /** 기존 공급: WIP_DISCRETE_JOBS 또는 처리대기 WIP_JOB_SCHEDULE_INTERFACE */
    public static class Supply {
        public String source;          // WIP | INTERFACE
        public long id;                // wip_entity_id | interface_id
        public String ref;             // 작업지시 번호
        public long itemId;
        public String itemCode;
        public String itemDesc;
        public int statusType;
        public String status;
        public double remainQty;
        public LocalDate start;
        public LocalDate completion;
        public boolean plannerCreated;
        public String resourceKey;
        public String note;
        public Map<String, Double> daily = new TreeMap<>();
    }

    /** 계획(제안) 작업지시 */
    public static class PlannedJob {
        public String id;
        public long soLineId;
        public String orderNumber;
        public String lineRef;
        public String customer;
        public long itemId;
        public String itemCode;
        public String itemDesc;
        public String uom;
        public double qty;
        public double suggestedQty;
        public LocalDate due;
        public LocalDate start;
        public LocalDate finish;
        public boolean late;
        public boolean releasable;
        public String resourceKey;
        public double hours;
        public String note;
        /** 구매부품이 모두 가용해지는 날 (자재 대기로 시작이 밀린 경우) */
        public LocalDate materialReady;
        public List<String> materialNotes = new ArrayList<>();
        public Map<String, Double> daily = new TreeMap<>();
    }

    // ---------- 재고 / 구매 / 자재 ----------

    /** MTL_ONHAND_QUANTITIES_DETAIL 집계 */
    public static class OnhandRow {
        public long itemId;
        public String itemCode;
        public String itemDesc;
        public String uom;
        public String makeBuy;
        public String subinventory;
        public String subinventoryDesc;
        public boolean nettable;
        public String locator;
        public String lot;
        public String revision;
        public double qty;
    }

    /** MTL_SYSTEM_ITEMS_B 계획 속성 (+ 플래너 수정값) */
    public static class ItemPlanning {
        public long itemId;
        public String itemCode;
        public String itemDesc;
        public String uom;
        public int makeBuy;           // 1=제조, 2=구매
        public double ebsPre;
        public double ebsFull;
        public double ebsPost;
        public double pre;
        public double full;
        public double post;
        public boolean overridden;
        public double minOrder;
        public double lotMultiplier;
        public double totalLt;
    }

    /** PO_LINE_LOCATIONS_ALL 미입고 납품일정 */
    public static class PoShipment {
        public long lineLocationId;
        public String poNumber;
        public String lineRef;
        public String supplier;
        public String status;
        public long itemId;
        public String itemCode;
        public String itemDesc;
        public String poUom;
        public String primaryUom;
        public double openQty;
        public LocalDate needBy;
        public LocalDate promised;
        public LocalDate overrideDate;
        public String overrideNote;
        /** 적용 입고예정일: 수정값 > 약속일 > 필요일 */
        public LocalDate expected;
        public String dateSource;
        /** 입고 후 후처리 리드타임까지 반영한 가용일 */
        public LocalDate available;
        public boolean pastDue;
    }

    public static class PurchasingData {
        public Org org;
        public List<PoShipment> shipments = new ArrayList<>();
        public List<ItemPlanning> items = new ArrayList<>();
    }

    /** BOM_COMPONENTS_B 1단계 */
    public static class BomComponent {
        public long assemblyId;
        public long componentId;
        public double qtyPer;
        public double yield;
    }

    /** WIP_REQUIREMENT_OPERATIONS 미출고 소요 */
    public static class WipRequirement {
        public long itemId;
        public String jobName;
        public LocalDate dateRequired;
        public double openQty;
    }

    /** 구매부품별 수급 요약 */
    public static class MaterialRow {
        public long itemId;
        public String itemCode;
        public String itemDesc;
        public String uom;
        public double onhand;
        public double poQty;
        public double wipReq;
        public double plannedReq;
        public double suggestedQty;
        public double shortage;
        public double totalLt;
        public LocalDate firstShortDate;
    }

    /** 구매 제안 */
    public static class PurchaseSuggestion {
        public long itemId;
        public String itemCode;
        public String itemDesc;
        public String uom;
        public double shortQty;
        public double qty;
        public LocalDate needDate;
        public LocalDate orderBy;
        public LocalDate receiptDate;
        public LocalDate availableDate;
        public boolean late;
        public String forRef;
    }

    public static class Adjust {
        public Double qty;
        public String start;
    }

    public static class PlanRequest {
        public long orgId;
        public String start;
        public int days = 28;
        public boolean netOnhand = true;
        public Map<String, Adjust> overrides = new LinkedHashMap<>();
    }

    public static class ReleaseRequest extends PlanRequest {
        public List<String> ids = new ArrayList<>();
        public Integer statusType;
        public String classCode;
    }

    public static class Day {
        public LocalDate date;
        public boolean work;
        public Day(LocalDate date, boolean work) { this.date = date; this.work = work; }
    }

    public static class PlanResult {
        public Org org;
        public LocalDate start;
        public LocalDate end;
        public List<Day> days = new ArrayList<>();
        public List<Resource> resources = new ArrayList<>();
        public List<Supply> existing = new ArrayList<>();
        public List<PlannedJob> planned = new ArrayList<>();
        public List<Demand> demands = new ArrayList<>();
        public List<MaterialRow> materials = new ArrayList<>();
        public List<PurchaseSuggestion> purchases = new ArrayList<>();
        public List<String> warnings = new ArrayList<>();
    }

    public static class ReleaseResult {
        public long groupId;
        public int count;
        public Long requestId;
        public boolean demoProcessed;
    }

    public static class InterfaceRow {
        public long groupId;
        public long interfaceId;
        public Long sourceLineId;
        public String itemCode;
        public double qty;
        public LocalDate startDate;
        public int phase;
        public int processStatus;
        public String status;
        public String jobName;
        public Long wipEntityId;
        public LocalDate created;
        public String description;
        public List<String> errors = new ArrayList<>();
    }

    public static class MasterData {
        public Org org;
        public List<Resource> resources = new ArrayList<>();
        public List<ItemRouting> routings = new ArrayList<>();
        public List<LocalDate> nonWorkDays = new ArrayList<>();
    }
}
