public final class TestMonitor {
 public static void main(String... args){
  if(!new cfr().a(12345L).l)throw new AssertionError("1.13 monitor not recovered");
  if(!new cty().a(23456L).l)throw new AssertionError("1.14 monitor not recovered");
  if(new cty().a(0L)!=null)throw new AssertionError("invalid monitor should remain null");
  System.out.println("PASS: 1.13 and 1.14 monitor cache recovery");
 }
}