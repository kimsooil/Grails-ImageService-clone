<?php
class ImageServiceClient {

    public static function getImageURL($imageServiceHost, $imageServicePort, $usfid, $keyName, $keyData){

        $imageServiceLoc = 'http://localhost:8080/ImageService/view/';

        // Create the plaintext containing the current time and the Unumber requested
        $plaintext = time() . '|' . $usfid;

        //Encrypt the token
        $encryptedToken = ImageServiceClient::encrypt($plaintext, $keyData);
        return 'https://'.$imageServiceHost.':'.$imageServicePort.'/ImageService/view/'.$keyName.'/'.urlencode($encryptedToken).'.jpg';
    }

    public static function getResizedImageURL($imageServiceHost, $imageServicePort, $usfid, $keyName, $keyData, $width, $height){

        $imageServiceLoc = 'http://localhost:8080/ImageService/view/';

        // Create the plaintext containing the current time and the Unumber requested
        $plaintext = time() . '|' . $usfid;

        //Encrypt the token
        $encryptedToken = ImageServiceClient::encrypt($plaintext, $keyData);
        return 'https://'.$imageServiceHost.':'.$imageServicePort.'/ImageService/view/'.$keyName.'/'.$width.'/'.$height.'/'.urlencode($encryptedToken).'.jpg';
    }

    private static function encrypt($input, $key, $filename_safe = TRUE) {
        srand((double) microtime() * 1000000); //for MCRYPT_RAND

        $size = mcrypt_get_block_size(MCRYPT_RIJNDAEL_256, MCRYPT_MODE_CBC);
        $input = ImageServiceClient::pkcs5_pad($input, $size);

        $td = mcrypt_module_open(MCRYPT_RIJNDAEL_256, '', MCRYPT_MODE_CBC, '');
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
}
?>
