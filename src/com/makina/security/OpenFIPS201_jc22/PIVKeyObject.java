/******************************************************************************
MIT License

  Project: OpenFIPS201
Copyright: (c) 2017 Commonwealth of Australia
   Author: Kim O'Sullivan - Makina (kim@makina.com.au)

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
******************************************************************************/

package com.makina.security.OpenFIPS201_jc22;

import javacard.framework.JCSystem;
import javacardx.crypto.Cipher;
import javacard.framework.*;

/**
 * Provides functionality for PIV key objects
 */
public abstract class PIVKeyObject extends PIVObject {

    protected static final short HEADER_MECHANISM			= (short)3;
    protected static final short HEADER_ROLE				= (short)4;

    private static final short FLAGS_AUTHENTICATED 			= (short)0;
    private static final short LENGTH_FLAGS 				= (short)1;

	//
	// Key Roles
	//
	// The following key roles are defined as control bitmap flags, meaning multiple can be
	// set at once.
	//
	
	// No special roles are defined by this key
    public static final byte ROLE_NONE						= (byte)0x00;
    
    // This key can be used for administrative authentication
    public static final byte ROLE_ADMIN						= (byte)0x01;
    
    // This key can be used for external authentication
    public static final byte ROLE_AUTH_EXTERNAL				= (byte)0x02;
    
    // This key can be used for internal authentication
    public static final byte ROLE_AUTH_INTERNAL				= (byte)0x04;
    
    // This key can only be generated on-card (i.e. injection is blocked)
    public static final byte ROLE_GENERATE_ONLY				= (byte)0x08;

    // Transient declaration
    private boolean[] securityFlags;

    protected PIVKeyObject(byte id, byte modeContact, byte modeContactless, byte mechanism, byte role) {

        super(id, modeContact, modeContactless);

        header[HEADER_MECHANISM] = mechanism;
        header[HEADER_ROLE] = role;

        securityFlags = JCSystem.makeTransientBooleanArray(LENGTH_FLAGS, JCSystem.CLEAR_ON_DESELECT);

        resetSecurityStatus();
    }

    public static PIVKeyObject create(byte id, byte modeContact, byte modeContactless, byte mechanism, byte role) {

        PIVKeyObject key;

        switch (mechanism) {

        case PIV.ID_ALG_DEFAULT:
        case PIV.ID_ALG_TDEA_3KEY:
        case PIV.ID_ALG_AES_128:
        case PIV.ID_ALG_AES_192:
        case PIV.ID_ALG_AES_256:
            key = new PIVKeyObjectSYM(id, modeContact, modeContactless, mechanism, role);
            //if (Config.FEATURE_PIV_TEST_VECTORS) ((PIVKeyObjectSYM)key).setKey(Config.TEST_VECTOR_KEY, (short)0);
            return key;

        case PIV.ID_ALG_RSA_1024:
        case PIV.ID_ALG_RSA_2048:
            key = new PIVKeyObjectPKI(id, modeContact, modeContactless, mechanism, role);
            return key;

        default:
            ISOException.throwIt(ISO7816.SW_FUNC_NOT_SUPPORTED);
            return null; // Keep the compiler happy
        }
    }

    public boolean match(byte id, byte mechanism) {
        return (header[HEADER_ID] == id && header[HEADER_MECHANISM] == mechanism);
    }

    public short getBlockLength() {

        switch (header[HEADER_MECHANISM]) {

        case PIV.ID_ALG_DEFAULT:
        case PIV.ID_ALG_TDEA_3KEY:
            return (short)8;

        case PIV.ID_ALG_AES_128:
        case PIV.ID_ALG_AES_192:
        case PIV.ID_ALG_AES_256:
            return (short)16;

        case PIV.ID_ALG_RSA_1024:
            return (short)128;

        case PIV.ID_ALG_RSA_2048:
            return (short)256;

        default:
            ISOException.throwIt(ISO7816.SW_DATA_INVALID);
            return (short)0; // Keep compiler happy
        }
    }

    public short getKeyLength() {
        switch (header[HEADER_MECHANISM]) {

        case PIV.ID_ALG_DEFAULT:
        case PIV.ID_ALG_TDEA_3KEY:
            return (short)24;

        case PIV.ID_ALG_AES_128:
            return (short)16;

        case PIV.ID_ALG_AES_192:
            return (short)24;

        case PIV.ID_ALG_AES_256:
            return (short)32;

        case PIV.ID_ALG_RSA_1024:
            return (short)128;

        case PIV.ID_ALG_RSA_2048:
            return (short)256;

        default:
            ISOException.throwIt(ISO7816.SW_DATA_INVALID);
            return (short)0; // Keep compiler happy
        }
    }

    public boolean isAsymmetric() {

        switch (header[HEADER_MECHANISM]) {

        case PIV.ID_ALG_DEFAULT:
        case PIV.ID_ALG_TDEA_3KEY:
        case PIV.ID_ALG_AES_128:
        case PIV.ID_ALG_AES_192:
        case PIV.ID_ALG_AES_256:
            return false;

        case PIV.ID_ALG_RSA_1024:
        case PIV.ID_ALG_RSA_2048:
            return true;

        default:
            ISOException.throwIt(ISO7816.SW_DATA_INVALID);
            return false; // Keep compiler happy
        }
    }

    public final byte getMechanism() {
        return header[HEADER_MECHANISM];
    }

    public final byte getRoles() {
        return header[HEADER_ROLE];
    }

    public final boolean hasRole(byte role) {
        return ((header[HEADER_ROLE] & role) == role);
    }

    public final void resetSecurityStatus() {
        securityFlags[FLAGS_AUTHENTICATED] = false;
    }

    public final void setSecurityStatus() {
        securityFlags[FLAGS_AUTHENTICATED] = true;
    }

    public final boolean getSecurityStatus() {
        return (securityFlags[FLAGS_AUTHENTICATED] == true);
    }

    public abstract void updateElement(byte element, byte[] buffer, short offset, short length);
    public abstract short encrypt(Cipher cipher, byte[] inBuffer, short inOffset, short inLength, byte[] outBuffer, short outOffset);
    public abstract short decrypt(Cipher cipher, byte[] inBuffer, short inOffset, short inLength, byte[] outBuffer, short outOffset);

}