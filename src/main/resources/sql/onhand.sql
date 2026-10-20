-- 가용(Nettable) 보관장소 현재고
SELECT moqd.inventory_item_id,
       SUM(moqd.primary_transaction_quantity) AS onhand_quantity
  FROM mtl_onhand_quantities_detail moqd
  JOIN mtl_secondary_inventories msub ON msub.secondary_inventory_name = moqd.subinventory_code
                                     AND msub.organization_id          = moqd.organization_id
 WHERE moqd.organization_id   = :orgId
   AND msub.availability_type = 1
 GROUP BY moqd.inventory_item_id
