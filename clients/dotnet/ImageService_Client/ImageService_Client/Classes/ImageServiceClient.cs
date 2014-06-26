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
		/// <param name="host">The hostname of the ImageService server.</param>
		/// <param name="port">The port of the ImageService service.</param>
		/// <param name="path">The base path of the ImageService service.</param>
		/// <param name="keyName">Name of the encryption key that will be used.</param>
		/// <param name="keyData">Encryption key.</param>
		/// <param name="usfid">USFid number for the requested image.</param>
		public static String getImageUrl(String host, int port, String path, String keyName, String keyData, String usfid) {
		
			String unixTimestamp = ( (int) (DateTime.UtcNow.Subtract(new DateTime(1970, 1, 1))).TotalSeconds).ToString();

			String plaintext = unixTimestamp + "|" + usfid;  

			String encryptedToken = ImageServiceClient.encrypt(keyData, plaintext);

			UriBuilder uri = new UriBuilder();
			uri.Host = host;
			uri.Port = port;
			uri.Path = path + "/view/" + keyName + "/" + encryptedToken + ".jpg";
			uri.Scheme = "https";
			return uri.ToString(); 
		}

		/// <summary>
		/// Construct a URL to retreive an image for the specified USFid and resize the image to specific height/width values.
		/// </summary>
		/// <param name="host">The hostname of the ImageService server.</param>
		/// <param name="port">The port of the ImageService service.</param>
		/// <param name="path">The base path of the ImageService service.</param>
		/// <param name="keyName">Name of the encryption key that will be used.</param>
		/// <param name="keyData">Encryption key.</param>
		/// <param name="usfid">USFid number for the requested image.</param>
		/// <param name="width">Image width in pixels.</param>
		/// <param name="height">Image height in pixels.</param>
		public static String getResizedImageUrl(String host, int port, String path, String keyName, String keyData, String usfid, int width, int height) {

			String unixTimestamp = ( (int) (DateTime.UtcNow.Subtract(new DateTime(1970, 1, 1))).TotalSeconds).ToString();

			String plaintext = unixTimestamp + "|" + usfid;  

			String encryptedToken = ImageServiceClient.encrypt(keyData, plaintext);

			UriBuilder uri = new UriBuilder();
			uri.Host = host;
			uri.Port = port;
			uri.Path = path + "/view/" + keyName + "/" + width + "/" + height + "/" + encryptedToken + ".jpg";
			uri.Scheme = "https";
			return uri.ToString(); 
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


