# TorJniJava

Java/Android JNI library for TOR based connections. The library wraps libtor.so (use your own build for security), pdnsd and tun2socks to create stable VPN connection over tor.

The project is split into multiple sub-libraries, so you can use only the needed parts in your application.

The library shall be battle tested in multiple large apps, however no development was made since 2021.

Pull requests are welcome.

TLDR;
Why another TOR library? I've ever wanted to use TOR connections in multiple of my Android apps, but most of the public libs use running TOR process in background.
This was an issue in reliability and after security fixes of Android, this solution was unusable. So I decided to give it a try and built the library as
shared object, which turned out to be working. Star this repo, if this lib was helpful.

## License

See [LICENSE](LICENSE).
