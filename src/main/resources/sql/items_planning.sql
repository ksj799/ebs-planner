-- 품목 계획 속성: 리드타임(일, 근무일 기준), 최소주문량, 배수
SELECT msi.inventory_item_id,
       msi.segment1                            AS item_code,
       msi.description                         AS item_desc,
       msi.primary_uom_code,
       NVL(msi.planning_make_buy_code, 2)      AS planning_make_buy_code,
       NVL(msi.preprocessing_lead_time, 0)     AS preprocessing_lead_time,
       NVL(msi.full_lead_time, 0)              AS full_lead_time,
       NVL(msi.postprocessing_lead_time, 0)    AS postprocessing_lead_time,
       NVL(msi.minimum_order_quantity, 0)      AS minimum_order_quantity,
       NVL(msi.fixed_lot_multiplier, 0)        AS fixed_lot_multiplier
  FROM mtl_system_items_b msi
 WHERE msi.organization_id = :orgId
   AND msi.inventory_item_id IN (:ids)
