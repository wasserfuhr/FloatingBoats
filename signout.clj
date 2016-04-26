(fn[rq rs](do
 (.invalidate(.getSession rq))
 (.sendRedirect rs"/")))
