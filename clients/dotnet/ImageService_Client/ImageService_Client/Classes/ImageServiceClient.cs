using System;
using System.Text;
using System.Security.Cryptography;
using System.Web;

namespace ImageService_Client
{

	public class ImageServiceClient
	{

		/// <summary>
		/// Construct a URL to retreive an image for the specified USFid.
		/// </summary>
		/// <param name="scheme">The scheme of the ImageService server connection (http/https).</param>
		/// <param name="host">The hostname of the ImageService server.</param>
		/// <param name="port">The port of the ImageService service.</param>
 		/// <param name="path">The base path of the ImageService service.</param>
		/// <param name="separator">Separtor charctore between itimestamp and identifier</param>
		/// <param name="keyName">Name of the encryption key that will be used.</param>
		/// <param name="keyData">Encryption key.</param>
		/// <param name="identifier">identifier for the requested image.</param>
		public static String getImageUrl(String scheme, String host, int port, String path, String separator, String keyName, String keyData, String identifier) {
		
			String unixTimestamp = ( (int) (DateTime.UtcNow.Subtract(new DateTime(1970, 1, 1))).TotalSeconds).ToString();

			String plaintext = unixTimestamp + separator + identifier;  

			String encryptedToken = ImageServiceClient.encrypt(keyData, plaintext);

			UriBuilder uri = new UriBuilder();
			uri.Host = host;
			uri.Port = port;
			uri.Path = path + "/view/" + keyName + "/" + encryptedToken + ".jpg";
			uri.Scheme = scheme;
			return uri.ToString(); 
		}

		/// <summary>
		/// Construct a URL to retreive an image for the specified USFid.
		/// </summary>
		/// <param name="host">The hostname of the ImageService server.</param>
		/// <param name="port">The port of the ImageService service.</param>
		/// <param name="path">The base path of the ImageService service.</param>
		/// <param name="separator">Separtor charctore between itimestamp and identifier</param>
		/// <param name="keyName">Name of the encryption key that will be used.</param>
		/// <param name="keyData">Encryption key.</param>
		/// <param name="identifier">identifier for the requested image.</param>
		public static String getImageUrl(String host, int port, String path, String separator, String keyName, String keyData, String identifier) {
			return getImageUrl("https", host, port, path, separator, keyName, keyData, identifier);
		}

		/// <summary>
		/// Construct a URL to retreive an image for the specified USFid.
		/// </summary>
		/// <param name="host">The hostname of the ImageService server.</param>
		/// <param name="path">The base path of the ImageService service.</param>
		/// <param name="keyName">Name of the encryption key that will be used.</param>
		/// <param name="keyData">Encryption key.</param>
		/// <param name="identifier">identifier for the requested image.</param>
		public static String getImageUrl(String host, String path, String keyName, String keyData, String identifier) {
			return getImageUrl("https", host, 443, path, "|", keyName, keyData, identifier);
		}

		/// <summary>
		/// Construct a URL to retreive an image for the specified USFid and resize the image to specific height/width values.
		/// </summary>
		/// <param name="scheme">The scheme of the ImageService server connection (http/https).</param>
		/// <param name="host">The hostname of the ImageService server.</param>
		/// <param name="port">The port of the ImageService service.</param>
		/// <param name="path">The base path of the ImageService service.</param>
		/// <param name="separator">Separtor charctore between itimestamp and identifier</param>
		/// <param name="keyName">Name of the encryption key that will be used.</param>
		/// <param name="keyData">Encryption key.</param>
		/// <param name="identifier">identifier for the requested image.</param>
		/// <param name="width">Image width in pixels.</param>
		/// <param name="height">Image height in pixels.</param>
		public static String getResizedImageUrl(String scheme, String host, int port, String path, String separator, String keyName, String keyData, String identifier, int width, int height) {

			String unixTimestamp = ( (int) (DateTime.UtcNow.Subtract(new DateTime(1970, 1, 1))).TotalSeconds).ToString();

			String plaintext = unixTimestamp + separator + identifier;  

			String encryptedToken = ImageServiceClient.encrypt(keyData, plaintext);

			UriBuilder uri = new UriBuilder();
			uri.Host = host;
			uri.Port = port;
			uri.Path = path + "/view/" + keyName + "/" + width + "/" + height + "/" + encryptedToken + ".jpg";
			uri.Scheme = scheme;
			return uri.ToString(); 
		}

		/// <summary>
		/// Construct a URL to retreive an image for the specified USFid and resize the image to specific height/width values.
		/// </summary>
		/// <param name="host">The hostname of the ImageService server.</param>
		/// <param name="port">The port of the ImageService service.</param>
		/// <param name="path">The base path of the ImageService service.</param>
		/// <param name="separator">Separtor charctore between itimestamp and identifier</param>
		/// <param name="keyName">Name of the encryption key that will be used.</param>
		/// <param name="keyData">Encryption key.</param>
		/// <param name="identifier">identifier for the requested image.</param>
		/// <param name="width">Image width in pixels.</param>
		/// <param name="height">Image height in pixels.</param>
		public static String getResizedImageUrl(String host, int port, String path, String separator, String keyName, String keyData, String identifier, int width, int height) {
			return getResizedImageUrl("https", host, port, path, separator, keyName, keyData, identifier, width, height);
		}

		/// <summary>
		/// Construct a URL to retreive an image for the specified USFid and resize the image to specific height/width values.
		/// </summary>
		/// <param name="host">The hostname of the ImageService server.</param>
		/// <param name="path">The base path of the ImageService service.</param>
		/// <param name="keyName">Name of the encryption key that will be used.</param>
		/// <param name="keyData">Encryption key.</param>
		/// <param name="identifier">identifier for the requested image.</param>
		/// <param name="width">Image width in pixels.</param>
		/// <param name="height">Image height in pixels.</param>
		public static String getResizedImageUrl(String host, String path, String keyName, String keyData, String identifier, int width, int height) {
			return getResizedImageUrl("https", host, 443, path, "|", keyName, keyData, identifier, width, height);
		}

		/// <summary>
		/// Encrypt the specified data using the provided key. The
		/// encrypted data will be returned as a base 64 encoded
		/// string.
		/// </summary>
		/// <param name="data">The data string to encrypt.</param>
		/// <param name="key">The key to use for encryption.</param>
		private static String encrypt(String key, String data){
			// Generate an initialization vector.
			RNGCryptoServiceProvider rng = new RNGCryptoServiceProvider();
			byte[] iv = new byte[16];
			rng.GetNonZeroBytes(iv);

			var secretKeyBytes = System.Text.Encoding.ASCII.GetBytes(key);

			byte[] dataBytes = System.Text.Encoding.ASCII.GetBytes(data);

			RijndaelManaged cipher = new RijndaelManaged{
				Mode = CipherMode.CBC,
				Padding = PaddingMode.PKCS7,
				KeySize = 256,
				BlockSize = 128,
				Key = secretKeyBytes,
				IV = iv
			};

			ICryptoTransform crypto = cipher.CreateEncryptor();
			byte[] encryptedData = crypto.TransformFinalBlock(dataBytes, 0, dataBytes.Length);

			byte[] finalOutput = new byte[iv.Length + encryptedData.Length];
			Array.Copy(iv, 0, finalOutput, 0, iv.Length);
			Array.Copy(encryptedData, 0, finalOutput, iv.Length, encryptedData.Length);

			return System.Convert.ToBase64String(finalOutput).Replace("+", "_").Replace("/", "-").Replace("=","");

		}
	}
}


