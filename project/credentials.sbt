credentials ++= {
  val bintrayUser = System.getenv("BINTRAY_USER")
  val bintrayPass = System.getenv("BINTRAY_PASS")
  if (bintrayUser == null || bintrayPass == null) Nil
  else Seq(Credentials("Bintray API Realm", "api.bintray.com", bintrayUser, bintrayPass))
}