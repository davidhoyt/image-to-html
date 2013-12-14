import SimpleSettings._

primarySettings := primary(
    name             = "image-to-html"
  , companyName      = "HoytSoft"
  , organization     = "org.github.davidhoyt"
  , homepage         = "https://github.com/davidhoyt/image-to-html"
  , vcsSpecification = "git@github.com:davidhoyt/image-to-html.git"
)

mavenSettings := maven(
  license(
      name  = "The Apache Software License, Version 2.0"
    , url   = "http://www.apache.org/licenses/LICENSE-2.0.txt"
  ),
  developer(
      id              = "David Hoyt"
    , name            = "David Hoyt"
    , email           = "dhoyt@guidewire.com"
    , url             = "http://www.hoytsoft.org/"
    , organization    = "Guidewire"
    , organizationUri = "http://www.guidewire.com/"
    , roles           = Seq("architect", "developer")
  )
)

publishSettings := publishing(
    releaseCredentialsID  = "sonatype-releases"
  , releaseRepository     = "https://oss.sonatype.org/service/local/staging/deploy/maven2"
  , snapshotCredentialsID = "sonatype-snapshots"
  , snapshotRepository    = "https://oss.sonatype.org/content/repositories/snapshots"
)

