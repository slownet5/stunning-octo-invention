package com.cryptopaths.cryptofm.encryption;

import java.io.File;

/**
 * Created by osama on 10/13/16.
 */

public interface EncryptionOperation {
    public void encryptFile(File inputFile,File outputFile, File keyFile)throws Exception ;
    public String decryptFile(File inputFile,File outputFile,File pubKeyFile,File secKeyFile, char[] pass)throws Exception;
}
