/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.usf.cims.idcard.ImageFetcher

/**
 *
 * @author james
 */
class crc {
  public static final int Polynomial = 0x04c11db7
  
	public static void main(String[] args) {
    System.out.print("CRC: " + Integer.toHexString(CreateCRC(new File(args[0]))))
    return
  }
  //###############################################################
  //# Creates CRC and returns it to the caller 
  //###############################################################
  public static int CreateCRC(File f) {
    int[] crc32_table = InitCRCTable()
    int crc = 0xFFFFFFFF;
    try {
      // get the file size
      def fsize = f.length()     
      FileInputStream fin = new FileInputStream(f);
      byte[] buffer = new byte[(int)fsize];

      fin.read(buffer); 
      fin.close();

      for(int  i = 0; i < fsize; i++) {
        crc = (crc >>> 8) ^ crc32_table[(crc & 0xFF) ^ (buffer[i] & 0xFF)]
      }
    } catch(FileNotFoundException e) {
        System.out.println(e.getMessage());    
        return 0;            
    } catch(IOException e ) {
        System.out.println(e.getMessage());    
        return 0;            
    }
    return  Integer.toHexString((crc ^ 0xFFFFFFFF))
  }  
  //#######################################################################
  //# Reflect used by Initialize CRC function
  //# This function does swap with bits
  //#######################################################################
  public static int Reflect(int ref, byte ch) {
    int value = 0;
    // Swap bit 0 for bit 7
    // bit 1 for bit 6, etc.
    for(int i = 1; i < (ch + 1); i++) {
      if((ref & 1) == 0) 
      {/* do noting */ }
      else
        value |= 1 << (ch - i);

      ref >>>= 1;
    }
    return value;
  }  
  //#######################################################################
  //# Initialize the crc table
  //#######################################################################
  public static int[] InitCRCTable() {
    int[] crc32_table = new int[256]
    // 256 values representing ASCII character codes.
    for(int i = 0; i <= 0xFF; i++) {
      crc32_table[i] = Reflect(i, (byte)8) << 24;
      for (int j = 0; j < 8; j++)
        crc32_table[i] = (crc32_table[i] << 1) ^ (((crc32_table[i] & (1 << 31)) == 0) ? 0 : Polynomial);

      crc32_table[i] = Reflect(crc32_table[i], (byte)32);    
    }
    return crc32_table
  }

}

