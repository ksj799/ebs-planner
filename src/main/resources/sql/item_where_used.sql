-- 사용처: 이 품목을 구성품으로 쓰는 상위 품목 (주 BOM)
SELECT bsb.assembly_item_id                  AS item_id,
       msi.segment1                          AS item_code,
       msi.description                       AS item_desc,
       msi.primary_uom_code,
       NVL(msi.planning_make_buy_code, 2)    AS planning_make_buy_code,
       bcb.component_quantity,
       NVL(bcb.component_yield_factor, 1)    AS component_yield_factor,
       bcb.wip_supply_type
  FROM bom_components_b bcb
  JOIN bom_structures_b bsb   ON bsb.common_bill_sequence_id = bcb.bill_sequence_id
  JOIN mtl_system_items_b msi ON msi.inventory_item_id = bsb.assembly_item_id
                             AND msi.organization_id   = bsb.organization_id
 WHERE bsb.organization_id    = :orgId
   AND bcb.component_item_id  = :itemId
   AND bsb.alternate_bom_designator IS NULL
   AND bcb.implementation_date IS NOT NULL
   AND bcb.effectivity_date <= :today
   AND (bcb.disable_date IS NULL OR bcb.disable_date > :today)
 ORDER BY msi.segment1
