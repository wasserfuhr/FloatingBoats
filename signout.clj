(fn [request response]
 (do
  (.invalidate (.getSession request))
  (.sendRedirect response "/")))