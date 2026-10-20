-- 품목 조회 목록 (품번/설명 검색, 제조/구매 필터)
SELECT * FROM (
  SELECT msi.inventory_item_id,
         msi.segment1                       AS item_code,
         msi.description                    AS item_desc,
         msi.primary_uom_code,
         msi.item_type,
         msi.inventory_item_status_code,
         NVL(msi.planning_make_buy_code, 2) AS planning_make_buy_code,
         msi.planner_code,
         msi.mrp_planning_code,
         msi.inventory_planning_code,
         NVL(msi.preprocessing_lead_time, 0) + NVL(msi.full_lead_time, 0)
           + NVL(msi.postprocessing_lead_time, 0) AS total_lead_time
    FROM mtl_system_items_b msi
   WHERE msi.organization_id = :orgId
     AND (UPPER(msi.segment1) LIKE :q OR UPPER(msi.description) LIKE :q)
     AND NVL(msi.planning_make_buy_code, 2) IN (:makeBuy)
   ORDER BY msi.segment1)
 WHERE ROWNUM <= :limit
