ImageService
============

A simple Grails service for serving images from the ID Card Center.  We had a few requirements when developing this service
* Respect the "privacy" flag set in OASIS
* Resize all images to a standard size (200 x 200)
* Allow services to request resized images if needed
* Authenticate each service/request by using an authentication token
* Support multiple algorithms for generating an authentication token:
 * AES256
 * MD5-HMAC
 * SHA1-HMAC
 * SHA256-HMAC


 Accessing the service
-----
Each authorized service is issued a 32-character key which is used to encrypt the request (AES) or generate a Hash (MD5/SHA).

###AES
I have included libraries for Java, PHP and C# as examples of how to access the ImageService using AES encryption.  This is the most secure method for accessing images and is preferred if you are developing a new application that requires images.

** MORE INFO TO COME**

###MD5/SHA1/SHA256

** MORE INFO TO COME**
