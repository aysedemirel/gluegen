/*
 * Copyright (c) 2005 Sun Microsystems, Inc. All Rights Reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 * 
 * - Redistribution of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 * 
 * - Redistribution in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 * 
 * Neither the name of Sun Microsystems, Inc. or the names of
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 * 
 * This software is provided "AS IS," without a warranty of any kind. ALL
 * EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND WARRANTIES,
 * INCLUDING ANY IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A
 * PARTICULAR PURPOSE OR NON-INFRINGEMENT, ARE HEREBY EXCLUDED. SUN
 * MICROSYSTEMS, INC. ("SUN") AND ITS LICENSORS SHALL NOT BE LIABLE FOR
 * ANY DAMAGES SUFFERED BY LICENSEE AS A RESULT OF USING, MODIFYING OR
 * DISTRIBUTING THIS SOFTWARE OR ITS DERIVATIVES. IN NO EVENT WILL SUN OR
 * ITS LICENSORS BE LIABLE FOR ANY LOST REVENUE, PROFIT OR DATA, OR FOR
 * DIRECT, INDIRECT, SPECIAL, CONSEQUENTIAL, INCIDENTAL OR PUNITIVE
 * DAMAGES, HOWEVER CAUSED AND REGARDLESS OF THE THEORY OF LIABILITY,
 * ARISING OUT OF THE USE OF OR INABILITY TO USE THIS SOFTWARE, EVEN IF
 * SUN HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
 * 
 * You acknowledge that this software is not designed or intended for use
 * in the design, construction, operation or maintenance of any nuclear
 * facility.
 * 
 * Sun gratefully acknowledges that this software was originally authored
 * and developed by Kenneth Bradley Russell and Christopher John Kline.
 */

package com.sun.gluegen.runtime;
import java.security.*;

/** Provides information to autogenerated struct accessors about what
    kind of data model (32- or 64-bit) is being used by the currently
    running process. */

public class CPU {
  private static boolean is32Bit;

  static {
    NativeLibrary.ensureNativeLibLoaded();

    boolean done=false;

    // Try to use Sun's sun.arch.data.model first ..
    int bits = getPointerSizeInBits();
    if ( 32 == bits || 64 == bits ) {
        is32Bit = ( 32 == bits );
        done = true ;
    }

    if(!done) {
        // We don't seem to need an AccessController.doPrivileged() block
        // here as these system properties are visible even to unsigned
        // applets
        // Note: this code is replicated in StructLayout.java
        String os = System.getProperty("os.name").toLowerCase();
        String cpu = System.getProperty("os.arch").toLowerCase();

        if(!done) {
            if ((os.startsWith("windows") && cpu.equals("x86")) ||
                (os.startsWith("windows") && cpu.equals("arm")) ||
                (os.startsWith("linux") && cpu.equals("i386")) ||
                (os.startsWith("linux") && cpu.equals("x86")) ||
                (os.startsWith("mac os") && cpu.equals("ppc")) ||
                (os.startsWith("mac os") && cpu.equals("i386")) ||
                (os.startsWith("darwin") && cpu.equals("ppc")) ||
                (os.startsWith("darwin") && cpu.equals("i386")) ||
                (os.startsWith("sunos") && cpu.equals("sparc")) ||
                (os.startsWith("sunos") && cpu.equals("x86")) ||
                (os.startsWith("freebsd") && cpu.equals("i386")) ||
                (os.startsWith("hp-ux") && cpu.equals("pa_risc2.0"))) {
              is32Bit = true;
              done = true;
            }
        }

        if(!done) {
            if ((os.startsWith("windows") && cpu.equals("amd64")) ||
                (os.startsWith("linux") && cpu.equals("amd64")) ||
                (os.startsWith("linux") && cpu.equals("x86_64")) ||
                (os.startsWith("linux") && cpu.equals("ia64")) ||
                (os.startsWith("mac os") && cpu.equals("x86_64")) ||
                (os.startsWith("darwin") && cpu.equals("x86_64")) ||
                (os.startsWith("sunos") && cpu.equals("sparcv9")) ||
                (os.startsWith("sunos") && cpu.equals("amd64"))) {
              is32Bit = false;
              done = true;
            }
        }

        if(!done) {
          throw new RuntimeException("Please port CPU detection (32/64 bit) to your platform (" + os + "/" + cpu + ")");
        }
    }
  }

  public static boolean is32Bit() {
    return is32Bit;
  }

  public static native int getPointerSizeInBits();

}
