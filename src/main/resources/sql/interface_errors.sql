SELECT wie.interface_id, wie.error_type, wie.error
  FROM wip_interface_errors wie
 WHERE wie.interface_id IN (:ids)
