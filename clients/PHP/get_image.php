<?php
        include('ImageServiceClient.php');

        $imageServiceScheme = 'http';
        $imageServiceHost = 'localhost';
        $imageServicePort = 8080;
        $imageServicePath = '/ImageService';

        //Get the standard resolution (200x200) image
        echo ImageServiceClient::getImageURL($imageServiceScheme, $imageServiceHost, $imageServicePort, $imageServicePath, $argv[1], 'test', 'AfoaKlDM4AjVyjo38f0NOs4O6hXM1T32'). "\n";


        //Get a specific size image
        $width = 400;
        $height = 400;
        echo ImageServiceClient::getResizedImageURL($imageServiceScheme, $imageServiceHost, $imageServicePort, $imageServicePath, $argv[1], 'test', 'AfoaKlDM4AjVyjo38f0NOs4O6hXM1T32', $width, $height). "\n";
