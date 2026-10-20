-- 미출하 수주라인 (제조품목만)
SELECT ool.line_id,
       ooh.order_number,
       ool.line_number,
       ool.shipment_number,
       hp.party_name                                       AS customer_name,
       ool.inventory_item_id,
       msi.segment1                                        AS item_code,
       msi.description                                     AS item_desc,
       msi.primary_uom_code,
       ool.order_quantity_uom,
       ool.ordered_quantity - NVL(ool.shipped_quantity, 0) AS open_quantity,
       NVL(ool.schedule_ship_date, ool.request_date)       AS due_date
  FROM oe_order_lines_all ool
  JOIN oe_order_headers_all ooh  ON ooh.header_id = ool.header_id
  JOIN mtl_system_items_b msi    ON msi.inventory_item_id = ool.inventory_item_id
                                AND msi.organization_id   = ool.ship_from_org_id
  LEFT JOIN hz_cust_accounts hca ON hca.cust_account_id = ooh.sold_to_org_id
  LEFT JOIN hz_parties hp        ON hp.party_id         = hca.party_id
 WHERE ool.ship_from_org_id   = :orgId
   AND ool.open_flag          = 'Y'
   AND ool.booked_flag        = 'Y'
   AND ool.line_category_code = 'ORDER'
   AND msi.build_in_wip_flag  = 'Y'
   AND ool.ordered_quantity - NVL(ool.shipped_quantity, 0) > 0
   AND NVL(ool.schedule_ship_date, ool.request_date) <= :toDate
 ORDER BY NVL(ool.schedule_ship_date, ool.request_date), ooh.order_number, ool.line_number
