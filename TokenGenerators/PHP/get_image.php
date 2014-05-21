<?php
        include('ImageServiceClient.php');

        $imageServiceLoc = 'localhost:8080';

        //Key info shared with the Image Service
        $key = array('name' => 'test2', 'data' => 'ABCDEFGHIJKLMNOP' );

        //Get the standard resolution (200x200) image
        echo ImageServiceClient::getImageURL($imageServiceLoc, $argv[1], $key). "\n";


        //Get a specific size image
        $width = 400;
        $height = 400;
        echo ImageServiceClient::getResizedImageURL($imageServiceLoc, $argv[1], $key, $width, $height). "\n";

