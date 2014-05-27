<?php
        include('ImageServiceClient.php');

        $imageServiceHost = 'localhost';
        $imageServicePort = 8080;

        //Get the standard resolution (200x200) image
        echo ImageServiceClient::getImageURL($imageServiceHost, $imageServicePort ,$argv[1], 'test', 'AfoaKlDM4AjVyjo38f0NOs4O6hXM1T32'). "\n";


        //Get a specific size image
        $width = 400;
        $height = 400;
        echo ImageServiceClient::getResizedImageURL($imageServiceHost, $imageServicePort, $argv[1], 'test', 'AfoaKlDM4AjVyjo38f0NOs4O6hXM1T32', $width, $height). "\n";
