-- 현재고 상세: 품목 × 보관장소 × 로케이터 × 로트 × 리비전
SELECT moqd.inventory_item_id,
       msi.segment1                     AS item_code,
       msi.description                  AS item_desc,
       msi.primary_uom_code,
       NVL(msi.planning_make_buy_code, 2) AS planning_make_buy_code,
       moqd.subinventory_code,
       msub.description                 AS subinventory_desc,
       msub.availability_type,
       mil.concatenated_segments        AS locator,
       moqd.lot_number,
       moqd.revision,
       SUM(moqd.primary_transaction_quantity) AS onhand_quantity
  FROM mtl_onhand_quantities_detail moqd
  JOIN mtl_system_items_b msi         ON msi.inventory_item_id = moqd.inventory_item_id
                                     AND msi.organization_id   = moqd.organization_id
  JOIN mtl_secondary_inventories msub ON msub.secondary_inventory_name = moqd.subinventory_code
                                     AND msub.organization_id          = moqd.organization_id
  LEFT JOIN mtl_item_locations_kfv mil ON mil.inventory_location_id = moqd.locator_id
                                      AND mil.organization_id       = moqd.organization_id
 WHERE moqd.organization_id = :orgId
 GROUP BY moqd.inventory_item_id, msi.segment1, msi.description, msi.primary_uom_code, msi.planning_make_buy_code,
          moqd.subinventory_code, msub.description, msub.availability_type,
          mil.concatenated_segments, moqd.lot_number, moqd.revision
 ORDER BY msi.segment1, moqd.subinventory_code, mil.concatenated_segments, moqd.lot_number
