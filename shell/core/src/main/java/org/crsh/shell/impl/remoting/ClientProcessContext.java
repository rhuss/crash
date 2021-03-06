/*
 * Copyright (C) 2012 eXo Platform SAS.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.crsh.shell.impl.remoting;

import org.crsh.shell.ShellProcess;
import org.crsh.shell.ShellProcessContext;
import org.crsh.shell.ShellResponse;
import org.crsh.text.Chunk;

import java.io.IOException;
import java.util.ArrayList;

class ClientProcessContext implements ShellProcessContext {

  /** . */
  final ClientAutomaton client;

  /** . */
  final ShellProcess process;

  /** . */
  final ArrayList<Chunk> buffer;

  ClientProcessContext(ClientAutomaton client, ShellProcess process) {
    this.client = client;
    this.process = process;
    this.buffer = new ArrayList<Chunk>(1000);
  }

  /**
   * Ensure we have a recent size, the size is considered as recent if it's younger than 2 second, otherwise
   * send a get size message.
   */
  private void ensureSize() {
    if (System.currentTimeMillis() - client.last > 2000) {
      synchronized (this) {
        try {
          client.out.writeObject(ServerMessage.GET_SIZE);
          client.out.flush();
        }
        catch (Exception e) {
          //
        }
      }
    }
  }

  public int getWidth() {
    ensureSize();
    return client.getWidth();
  }

  public int getHeight() {
    ensureSize();
    return client.getHeight();
  }

  public boolean takeAlternateBuffer() {
    try {
      client.out.writeObject(ServerMessage.USE_ALTERNATE_BUFFER);
      client.out.flush();
    }
    catch (Exception e) {
      //
    }

    // For now we suppose any impl return true;
    return true;
  }

  public boolean releaseAlternateBuffer() {
    try {
      client.out.writeObject(ServerMessage.USE_MAIN_BUFFER);
      client.out.flush();
    }
    catch (Exception e) {
      //
    }

    // For now we suppose any impl return true;
    return true;
  }

  public String getProperty(String name) {
    return null;
  }

  public String readLine(String msg, boolean echo) {
//    try {
//      client.out.writeObject(ServerMessage.READLINE);
//      client.out.writeObject(msg);
//      client.out.writeObject(echo);
//      client.out.flush();
//      return (String)client.in.readObject();
//    }
//    catch (Exception e) {
//      return null;
//    }
    return null;
  }

  public void provide(Chunk element) throws IOException {
    buffer.add(element);
  }

  public synchronized void flush() {
    if (buffer.size() > 0) {
      try {
        for (Chunk chunk : buffer) {
          client.out.writeObject(ServerMessage.CHUNK);
          client.out.writeObject(chunk);
        }
        client.out.writeObject(ServerMessage.FLUSH);
        client.out.flush();
      }
      catch (IOException ignore) {
        //
      }
      finally {
        buffer.clear();
      }
    }
  }

  public synchronized void end(ShellResponse response) {

    // Flush what we have in buffer first
    flush();

    // Send end message
    try {
      client.current = null;
      client.out.writeObject(ServerMessage.END);
      client.out.writeObject(response);
      client.out.flush();
    }
    catch (IOException ignore) {
      //
    }
    finally {
      if (response instanceof ShellResponse.Close) {
        client.close();
      }
    }
  }
}
