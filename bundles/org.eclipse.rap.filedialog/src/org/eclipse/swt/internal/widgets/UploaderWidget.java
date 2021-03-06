/*******************************************************************************
 * Copyright (c) 2014, 2015 EclipseSource and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    EclipseSource - initial API and implementation
 ******************************************************************************/
package org.eclipse.swt.internal.widgets;

import org.eclipse.rap.rwt.widgets.FileUpload;


public class UploaderWidget implements Uploader {

  private final FileUpload fileUpload;

  public UploaderWidget( FileUpload fileUpload ) {
    this.fileUpload = fileUpload;
  }

  @Override
  public void submit( String url ) {
    fileUpload.submit( url );
  }

  @Override
  public void dispose() {
    if( !fileUpload.isDisposed() ) {
      fileUpload.dispose();
    }
  }

}
