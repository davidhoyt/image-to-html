import sbtassembly.Plugin._
import AssemblyKeys._
import SimpleSettings._

val applicationDist = TaskKey[File]("application-dist", "Creates a distributable zip file.")

val applicationDistProperties = Seq(
  applicationDist <<= (AssemblyKeys.assembly, Keys.target, Keys.name, Keys.version) map { (dist:File, target:File, name:String, version:String) =>
    val distdir = target / (name +"-"+ version)
    val zipFile = target / (name +"-"+ version +".zip")
    //Clear existing debris if it exists.
    IO.delete(zipFile)
    IO.delete(distdir)
    //
    val bin = distdir / "bin"
    //
    //Copy existing files from project root into the
    //specified directories.
    val dirs = Seq(bin)
    IO.createDirectories(dirs)
    dirs.foreach { dir =>
      IO.copyDirectory(file(".") / dir.getName, distdir / dir.getName, overwrite = true, preserveLastModified = true)
    }
    //Copy assembly to bin/
    val distFile = bin / dist.getName
    IO.copyFile(dist, distFile)
    //
    def entries(f: File):List[File] = f :: (if (f.isDirectory) IO.listFiles(f).toList.flatMap(entries(_)) else Nil)
    //
    IO.zip(entries(distdir).map(d => (d, d.getAbsolutePath.substring(distdir.getParent.length))), zipFile)
    zipFile
  }
)

lazy val _root = root("image-to-html", "image-to-html", PUBLISH_ARTIFACT)
                   .settings(assemblySettings: _*)
                   .settings(applicationDistProperties: _*)
