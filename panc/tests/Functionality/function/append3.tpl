#
# @expect="/profile/result='true'"
#
object template append3;

'/x' = list(1);

'/x' = append(SELF, 2);

'/result' = {
  x = value('/x');
  (length(x) == 2 && x[0] == 1 && x[1] == 2);
};

