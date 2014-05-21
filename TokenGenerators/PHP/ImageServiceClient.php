<?php
class ImageServiceClient {

    public static function getImageURL($ImageServiceHost, $usfid, $key){

        $imageServiceLoc = 'http://localhost:8080/ImageService/view/';

        // Create the plaintext containing the current time and the Unumber requested
        $plaintext = time() . '|' . $usfid;

        //Encrypt the token
        $encryptedToken = ImageServiceClient::encrypt($plaintext, $key['data']);
        return 'https://'.$ImageServiceHost.'/ImageService/view/'.$key['name'].'/'.urlencode($encryptedToken).'.jpg';
    }

    public static function getResizedImageURL($ImageServiceHost, $usfid, $key, $width, $height){

        $imageServiceLoc = 'http://localhost:8080/ImageService/view/';

        // Create the plaintext containing the current time and the Unumber requested
        $plaintext = time() . '|' . $usfid;

        //Encrypt the token
        $encryptedToken = ImageServiceClient::encrypt($plaintext, $key['data']);
        return 'https://'.$ImageServiceHost.'/ImageService/view/'.$key['name'].'/'.$width.'/'.$height.'/'.urlencode($encryptedToken).'.jpg';
    }

    private static function encrypt($input, $key, $filename_safe = TRUE) {
        srand((double) microtime() * 1000000); //for MCRYPT_RAND

        $size = mcrypt_get_block_size(MCRYPT_RIJNDAEL_128, MCRYPT_MODE_CBC); 
        $input = ImageServiceClient::pkcs5_pad($input, $size); 

        $td = mcrypt_module_open(MCRYPT_RIJNDAEL_128, '', MCRYPT_MODE_CBC, ''); 
        $iv = mcrypt_create_iv(mcrypt_enc_get_iv_size($td), MCRYPT_RAND); 

        mcrypt_generic_init($td, $key, $iv); 
        $data = mcrypt_generic($td, $input); 
        mcrypt_generic_deinit($td); 
        mcrypt_module_close($td); 

        $base64Data = base64_encode($iv.$data);

        if ($filename_safe){
            return rtrim(strtr($base64Data, '+/', '-_'), '='); 
        } else {
            return $base64Data;
        }
    } 

    private static function pkcs5_pad ($text, $blocksize) { 
        $pad = $blocksize - (strlen($text) % $blocksize); 
        return $text . str_repeat(chr($pad), $pad); 
    } 

    function pkcs5_unpad($text)  {
        $pad = ord($text{strlen($text)-1}); 
        if ($pad > strlen($text)) return false; 
        if (strspn($text, chr($pad), strlen($text) - $pad) != $pad) return false; 
        return substr($text, 0, -1 * $pad); 
    } 

    public static function decrypt($sStr, $sKey, $filename_safe = TRUE) {
      $td = mcrypt_module_open(MCRYPT_RIJNDAEL_128, '', MCRYPT_MODE_CBC, '');
  
      $str = base64_decode($sStr);
      $iv_size = mcrypt_enc_get_iv_size($td);
      $iv = substr($str,0,16);
      $string = substr($str,16);

      $decrypted= mcrypt_decrypt(
          MCRYPT_RIJNDAEL_128,
          $sKey, 
          $string, 
          MCRYPT_MODE_CBC,
          $iv
      );

     $dec_s = strlen($decrypted); 
     $padding = ord($decrypted[$dec_s-1]); 
     $decrypted = substr($decrypted, 0, -$padding);
     
     return $decrypted;
    }
}
?>
