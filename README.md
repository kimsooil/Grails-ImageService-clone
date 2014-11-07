ImageService
============

A simple Grails service for serving images from the a central repository of files.  It was developed to serve card 
images from the USF ID Card Center.  

We had a few requirements when developing this service:
* Respect the "privacy" flag set in the Student Information System
* All images are stored in a standard size, but different size images may be needed
* Authenticate each service/request by using an authentication token
* Support multiple algorithms for generating the token:
 * AES256
 * MD5
 * MD5-HMAC
 * SHA1
 * SHA1-HMAC
 * SHA256
 * SHA256-HMAC
 * SHA384
 * SHA384-HMAC
 * SHA512
 * SHA512-HMAC

---

#Accessing the service

To request an image, you need three pieces of data: the *__image identifier__*, the *__application identifier__* and 
the *__application key__*.

The image identifier is an alpha-numeric string (student/employee id, image number, etc) that will be used to request 
the image.  Each of the images in the photo repository should be named in the format *{identifier}.jpg*  

When a new application is authorized to request images from this service, they are given a short, human-readable 
identifier.  Think of this identifier as the "username" for the application accessing the image service.  This value is 
public and will be used in the URL of the image request.  

The application key is a 32-character key which is used to encrypt the request (AES) or generate a Hash (MD5/SHA).  
This is the application's password and must be guarded carefully!


##MD5/SHA

###Step 1: Generate the image filename
We've implemented multiple hashing algorithms to make integration easier, but are all very similar and you can pick the 
one that works best with your application's language.  The basic premise of using a hashing algorithm for authentication 
takes the identifier for an image (student number in our case) combines it with the application key and generates a 
"hash".  The filename of the image is constructed of the identifier and this hash 
(`U12345678-a9c74a2e556df234c270035883501be7ac925b2f.jpg`).  On the server end, we generate a hash using the same input 
values and if the hashes match, the image is returned.

When generating the hash, HMAC algorithms take two inputs: application key and identifier, while the non-HMAC algorithms 
accept a single input.  When using a non-HMAC hashing algorithm, the application key and the identifier should be 
concatenated together into a single string which will be the input to the hashing algorithm.

From a security standpoint SHA512-HMAC is considered the most secure and would be the preferred out of all the choices, 
but it is also the slowest and creates the longest hash values.  Therefore, you'll need to decide if that level of 
security is needed or if you can trade a bit of security for efficiency.

####Pros
* Very simple to implement
* Fast

####Cons
* The resulting filename never changes, making replay attacks a concern.
* The image identifier is exposed in the filename.

####Example (PHP)
```php
<?php

$identifier = 'U12345678';
$application_key = 'bfTkmK8CxKAQMABJ5Wg1xmqycvvxkdLa';

// Using SHA1-HMAC
$hash = hash_hmac('sha1', $identifier, $application_key);

// Outputs U12345678_61eca870a86cd0d8238c853c80ef5280d4c01640.jpg
echo $identifier."_".$hash.".jpg\n";

//Using SHA1
$input_string = $identifier.$application_key;
$hash = sha1($input_string);

// Outputs U12345678_464754eab8ce5642f9134e1684b7498220dd6188.jpg
echo $identifier."_".$hash.".jpg\n";

?>
```

###Step 2: Create the request URL

Now that we have the image filename, we need the rest of the URL.  To request an image in the standard size 
(200px x 200px), use this URL format:

```
https://{ImageServiceHost}/ImageService/view/{applicationIdentifer}/{imageFilename}

EXAMPLE:
https://localhost:8443/ImageService/view/myApplication/U12345678-a9c74a2e556df234c270035883501be7ac925b2f.jpg
```

and to request a resized (300px x 300px) image:
```
https://{ImageServiceHost}/ImageService/view/{applicationIdentifer}/{width}/{height}/{imageFilename}

EXAMPLE:
https://localhost:8443/ImageService/view/myApplication/300/300/U12345678-a9c74a2e556df234c270035883501be7ac925b2f.jpg
```
---

##AES
The other process for generating the image filename uses the AES256 encryption algorithm.  The first step is creating 
the "plaintext" that will be encrypted.  We do this by combining the current time as a Unix timestamp (seconds since 
1/1/1970) with the image identifier and then encrypting it using the application key as the password.
```
AES256(key:"AfoaKlDM4AjVyjo38f0NOs4O6hXM1T32", plaintext:"1403793319|U12345678") = DiHfS3Baw8xYjlXklYr0ciiaUB7uv1S4ZukImmNMnsJ4DPDMTW5niNch30ORmg9R7YfyjGi1Bi44gBp87517Eg
```

From this point on, the process of generating a request URL is the same as with the hashing algorithms.  To verify the 
filename, the server decrypts the value.  If it resulting plaintext is in the correct format, the timestamp is then 
compared to the server's clock.  If it is more than a certain number of seconds old (30 by default), the request is 
rejected.  If the request is recent enough, the image is returned.

Dealing with encryption, especially between programming languages, can be quit difficult.  To make implementation 
easier, I have included clients in the __clients__ for Java, PHP and C# as examples.  AES encryption is the most secure 
method for accessing images and is preferred if you are developing a new application that requires images.

####Pros
* The image identifier is protected and not visible to the client browser.
* The filename changes on each request and is only valid for a short period, making replay almost impossible.

####Cons
* Slower than hashing algorithms.
* Harder to implement.

###C# Example
```c#

using System;
using System.IO;

namespace ImageService_Client
{
	class MainClass
	{

		public static void Main(string[] args)
		{

			/// Set to the hostname of your ImageService server.
			string imageServiceHost = "localhost";
			/// Set to the path of the ImageService path
			string imageServicePath = "/ImageService";
			/// Set to the name of the key you will be sending.
			string keyName = "test";
			/// Set to the value of the key you will using for encryption.
			string keyData = "AfoaKlDM4AjVyjo38f0NOs4O6hXM1T32";
			/// Set to the Unumber you want to get the image for
			string usfid = "U44989263";

			string url = ImageServiceClient.getImageUrl (imageServiceHost, imageServicePath, keyName, keyData, usfid);
			Console.WriteLine (url);
		}
	}
}

```

###PHP Example
```php
<?php
        include('ImageServiceClient.php');

        $imageServiceScheme = 'https';
        $imageServiceHost = 'localhost';
        $imageServicePort = 8443;
        $imageServicePath = '/ImageService';

        //Get the standard resolution (200x200) image
        echo ImageServiceClient::getImageURL($imageServiceScheme, $imageServiceHost, $imageServicePort, $imageServicePath, $argv[1], 'test', 'AfoaKlDM4AjVyjo38f0NOs4O6hXM1T32'). "\n";

?>
```

###Java Example
```java
import edu.usf.cims.ImageServiceClient;
import java.net.URI;

public class ImageServiceTest{
	public static void main(String[] args) {

        String imageServiceHost = "localhost";
        int imageServicePort = 8443;
        String imageServicePath = "/ImageService";
        String serviceName = "test";
        String serviceKey = "AfoaKlDM4AjVyjo38f0NOs4O6hXM1T32";
        String usfid = "U44989263";

		URL imageURL = ImageServiceClient.getImageUrl(imageServiceHost, imageServicePort, imageServicePath, serviceName, serviceKey, usfid)

		System.out.println(imageURL.toString());
	}
}
```
