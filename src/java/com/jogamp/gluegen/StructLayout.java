/*
 * Copyright (c) 2003 Sun Microsystems, Inc. All Rights Reserved.
 * Copyright (c) 2010 JogAmp Community. All rights reserved.
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

package com.jogamp.gluegen;

import com.jogamp.common.os.Platform;
import com.jogamp.gluegen.cgram.types.*;

/** Encapsulates algorithm for laying out data structures. Note that
    this ends up embedding code in various places via SizeThunks. If
    the 32-bit and 64-bit ports on a given platform differ
    fundamentally in their handling of struct layout then this code
    will need to be updated and, most likely, two versions of the
    SizeThunks maintained in various places. */

public class StructLayout {
  private int baseOffset;
  private int structAlignment;

  protected StructLayout(int baseOffset,
                         int structAlignment) {
    this.baseOffset = baseOffset;
    this.structAlignment = structAlignment;
  }

  public void layout(CompoundType t) {
    int n = t.getNumFields();
    SizeThunk curOffset = SizeThunk.constant(baseOffset);
    SizeThunk maxSize   = SizeThunk.constant(0);
    for (int i = 0; i < n; i++) {
      Field f = t.getField(i);
      Type ft = f.getType();
      if (ft.isInt() || ft.isFloat() || ft.isDouble() || ft.isPointer()) {
        SizeThunk sz = ft.getSize();
        curOffset = SizeThunk.roundUp(curOffset, sz);
        f.setOffset(curOffset);
        if (t.isUnion()) {
          maxSize = SizeThunk.max(maxSize, sz);
        } else {
          curOffset = SizeThunk.add(curOffset, sz);
        }
      } else if (ft.isCompound()) {
        new StructLayout(0, structAlignment).layout(ft.asCompound());
        curOffset = SizeThunk.roundUp(curOffset, SizeThunk.constant(structAlignment));
        f.setOffset(curOffset);
        if (t.isUnion()) {
          maxSize = SizeThunk.max(maxSize, ft.getSize());
        } else {
          curOffset = SizeThunk.add(curOffset, ft.getSize());
        }
      } else if (ft.isArray()) {
        ArrayType arrayType = ft.asArray();
        CompoundType compoundElementType = arrayType.getBaseElementType().asCompound();
        if (compoundElementType != null) {
          new StructLayout(0, structAlignment).layout(compoundElementType);
          arrayType.recomputeSize();
        }
        // Note: not sure how this rounding is done
        curOffset = SizeThunk.roundUp(curOffset, SizeThunk.constant(structAlignment));
        f.setOffset(curOffset);
        curOffset = SizeThunk.add(curOffset, ft.getSize());
      } else {
        // FIXME
        String name = t.getName();
        if (name == null) {
          name = t.toString();
        }
        throw new RuntimeException("Complicated field types (" + ft +
                                   " " + f.getName() +
                                   " in type " + name +
                                   ") not implemented yet");
      }
    }
    // FIXME: I think the below is wrong; better check with some examples
    //    if ((curOffset % structAlignment) != 0) {
    //      curOffset += structAlignment - (curOffset % structAlignment);
    //    }
    if (t.isUnion()) {
      t.setSize(maxSize);
    } else {
      t.setSize(curOffset);
    }
  }

  

  /**
   * <P>See alignment in {@link com.jogamp.common.os.MachineDescription}.</p>
   * 
   * <P>The code is currently used at compile time {@link JavaEmitter#layoutStruct(CompoundType t)} once,
   * and code for structs is emitted for generic 32bit and 64bit only {@link JavaEmitter#emitStruct(CompoundType structType, String alternateName)}.</p>
   */
  public static StructLayout createForCurrentPlatform() {
    final Platform.OSType osType = Platform.getOSType();
    final Platform.CPUArch cpuArch = Platform.getCPUArch();
    
    if( ( Platform.OSType.WINDOWS == osType && Platform.CPUArch.X86_32 == cpuArch ) || // It appears that Windows uses a packing alignment of 4 bytes in 32-bit mode
        ( Platform.CPUArch.ARM_32 == cpuArch ) 
      ) {
      return new StructLayout(0, 4);
    } else if ((Platform.OSType.WINDOWS == osType && Platform.CPUArch.X86_64 == cpuArch) ||
               (Platform.OSType.LINUX == osType   && Platform.CPUArch.X86_32 == cpuArch) ||
               (Platform.OSType.LINUX == osType   && Platform.CPUArch.X86_64 == cpuArch) ||
               (Platform.OSType.LINUX == osType   && Platform.CPUArch.IA64 == cpuArch) ||
               (Platform.OSType.SUNOS == osType   && Platform.CPUArch.SPARC_32 == cpuArch) ||
               (Platform.OSType.SUNOS == osType   && Platform.CPUArch.SPARCV9_64 == cpuArch) ||
               (Platform.OSType.SUNOS == osType   && Platform.CPUArch.X86_32 == cpuArch) ||
               (Platform.OSType.SUNOS == osType   && Platform.CPUArch.X86_64 == cpuArch) ||
               (Platform.OSType.MACOS == osType   && Platform.CPUArch.PPC == cpuArch) ||
               (Platform.OSType.MACOS == osType   && Platform.CPUArch.X86_32 == cpuArch) ||
               (Platform.OSType.MACOS == osType   && Platform.CPUArch.X86_64 == cpuArch) ||
               (Platform.OSType.FREEBSD == osType && Platform.CPUArch.X86_32 == cpuArch) ||
               (Platform.OSType.FREEBSD == osType && Platform.CPUArch.X86_64 == cpuArch) ||
               (Platform.OSType.HPUX == osType    && Platform.CPUArch.PA_RISC2_0 == cpuArch)
               ) {
      return new StructLayout(0, 8);
    } else {
      // FIXME: add more ports
      throw new RuntimeException("Please port StructLayout to your OS (" + osType + ") and CPU (" + cpuArch + ")");
    }
  }
}
