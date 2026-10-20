-- 미입고 PO 납품일정 (취소·마감 제외)
SELECT pll.line_location_id,
       poh.segment1                       AS po_number,
       pol.line_num,
       pll.shipment_num,
       sup.vendor_name,
       NVL(poh.authorization_status, 'INCOMPLETE') AS authorization_status,
       pol.item_id,
       msi.segment1                       AS item_code,
       msi.description                    AS item_desc,
       pol.unit_meas_lookup_code          AS po_uom,
       msi.primary_unit_of_measure,
       pll.quantity - NVL(pll.quantity_received, 0) - NVL(pll.quantity_cancelled, 0) AS open_quantity,
       pll.need_by_date,
       pll.promised_date
  FROM po_line_locations_all pll
  JOIN po_lines_all pol       ON pol.po_line_id   = pll.po_line_id
  JOIN po_headers_all poh     ON poh.po_header_id = pll.po_header_id
  JOIN mtl_system_items_b msi ON msi.inventory_item_id = pol.item_id
                             AND msi.organization_id   = pll.ship_to_organization_id
  LEFT JOIN ap_suppliers sup  ON sup.vendor_id = poh.vendor_id
 WHERE pll.ship_to_organization_id = :orgId
   AND pll.shipment_type IN ('STANDARD', 'BLANKET', 'SCHEDULED')
   AND NVL(pll.cancel_flag, 'N') = 'N'
   AND NVL(pll.closed_code, 'OPEN') NOT IN ('CLOSED', 'FINALLY CLOSED', 'CLOSED FOR RECEIVING')
   AND pll.quantity - NVL(pll.quantity_received, 0) - NVL(pll.quantity_cancelled, 0) > 0
 ORDER BY NVL(pll.promised_date, pll.need_by_date), poh.segment1, pol.line_num, pll.shipment_num
