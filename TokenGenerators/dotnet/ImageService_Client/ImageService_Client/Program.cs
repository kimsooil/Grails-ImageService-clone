using System;
using System.IO;

namespace ImageService_Client
{
	class MainClass
	{
		/// <summary>
		/// An example program to generate the ImageService URL for a given Unumber
		/// </summary>
		public static void Main(string[] args)
		{
			/// Set to the hostname of your ImageService server.
			string imageServiceHost = "localhost";
			/// Set to the port of your ImageService server
			int imageServicePort = 8080;
			/// Set to the path of the ImageService path
			string imageServicePath = "/ImageService";
			/// Set to the name of the key you will be sending.
			string keyName = "test";
			/// Set to the value of the key you will using for encryption.
			string keyData = "AfoaKlDM4AjVyjo38f0NOs4O6hXM1T32";
			/// Set to the Unumber you want to get the image for
			string usfid = "U44989263";

			string url = ImageServiceClient.getImageUrl (imageServiceHost, imageServicePort, imageServicePath, keyName, keyData, usfid);
			Console.WriteLine ("Normal image;" + url);
			string resized = ImageServiceClient.getResizedImageUrl (imageServiceHost, imageServicePort, imageServicePath, keyName, keyData, usfid, 300, 300);
			Console.WriteLine ("Resized to 300x300:" + resized);
		}
	}
}
