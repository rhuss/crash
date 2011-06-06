package org.crsh.cmdline.matcher.impl;

import org.crsh.cmdline.matcher.CmdCompletionException;

import java.util.Map;

/**
 * @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a>
 */
abstract class Completion {

  abstract Map<String, String> complete() throws CmdCompletionException;

}
