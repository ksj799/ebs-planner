DECLARE
  l_request_id NUMBER;
BEGIN
  fnd_global.apps_initialize(?, ?, ?);
  l_request_id := fnd_request.submit_request('WIP', 'WICMLP', NULL, NULL, FALSE, ?, ?, ?);
  ? := l_request_id;
  IF l_request_id = 0 THEN
    ? := fnd_message.get;
  END IF;
END;
