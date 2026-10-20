-- 품목 수급 요약
SELECT (SELECT NVL(SUM(moqd.primary_transaction_quantity), 0)
          FROM mtl_onhand_quantities_detail moqd
          JOIN mtl_secondary_inventories msub ON msub.secondary_inventory_name = moqd.subinventory_code
                                             AND msub.organization_id          = moqd.organization_id
         WHERE moqd.organization_id = :orgId AND moqd.inventory_item_id = :itemId
           AND msub.availability_type = 1) AS onhand_nettable,
       (SELECT NVL(SUM(moqd.primary_transaction_quantity), 0)
          FROM mtl_onhand_quantities_detail moqd
          JOIN mtl_secondary_inventories msub ON msub.secondary_inventory_name = moqd.subinventory_code
                                             AND msub.organization_id          = moqd.organization_id
         WHERE moqd.organization_id = :orgId AND moqd.inventory_item_id = :itemId
           AND msub.availability_type <> 1) AS onhand_other,
       (SELECT NVL(SUM(pll.quantity - NVL(pll.quantity_received, 0) - NVL(pll.quantity_cancelled, 0)), 0)
          FROM po_line_locations_all pll
          JOIN po_lines_all pol ON pol.po_line_id = pll.po_line_id
         WHERE pll.ship_to_organization_id = :orgId AND pol.item_id = :itemId
           AND NVL(pll.cancel_flag, 'N') = 'N'
           AND NVL(pll.closed_code, 'OPEN') NOT IN ('CLOSED', 'FINALLY CLOSED', 'CLOSED FOR RECEIVING')) AS po_open,
       (SELECT NVL(SUM(wdj.start_quantity - NVL(wdj.quantity_completed, 0) - NVL(wdj.quantity_scrapped, 0)), 0)
          FROM wip_discrete_jobs wdj
         WHERE wdj.organization_id = :orgId AND wdj.primary_item_id = :itemId
           AND wdj.status_type IN (1, 3, 6)) AS wip_open,
       (SELECT NVL(SUM(ool.ordered_quantity - NVL(ool.shipped_quantity, 0)), 0)
          FROM oe_order_lines_all ool
         WHERE ool.ship_from_org_id = :orgId AND ool.inventory_item_id = :itemId
           AND ool.open_flag = 'Y' AND ool.booked_flag = 'Y' AND ool.line_category_code = 'ORDER') AS so_open
  FROM dual
