/*
 * Copyright (c) JForum Team
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, 
 * with or without modification, are permitted provided 
 * that the following conditions are met:
 * 
 * 1) Redistributions of source code must retain the above 
 * copyright notice, this list of conditions and the 
 * following  disclaimer.
 * 2)  Redistributions in binary form must reproduce the 
 * above copyright notice, this list of conditions and 
 * the following disclaimer in the documentation and/or 
 * other materials provided with the distribution.
 * 3) Neither the name of "Rafael Steil" nor 
 * the names of its contributors may be used to endorse 
 * or promote products derived from this software without 
 * specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT 
 * HOLDERS AND CONTRIBUTORS "AS IS" AND ANY 
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, 
 * BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF 
 * MERCHANTABILITY AND FITNESS FOR A PARTICULAR 
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL 
 * THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE 
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, 
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES 
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF 
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, 
 * OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER 
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER 
 * IN CONTRACT, STRICT LIABILITY, OR TORT 
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN 
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF 
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE
 * 
 * This file creation date: Nov 13, 2004 / 17:17:09
 * The JForum Project
 * http://www.jforum.net
 */

package net.jforum.util;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.awt.image.ImageFilter;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.imageio.ImageIO;

import net.jforum.JForumExecutionContext;
import net.jforum.SessionFacade;
import net.jforum.util.preferences.ConfigKeys;
import net.jforum.util.preferences.SystemGlobals;

import org.apache.commons.lang.ObjectUtils;
import org.apache.log4j.Logger;

import com.jhlabs.image.CheckFilter;
import com.jhlabs.image.CrystalizeFilter;
import com.jhlabs.image.DisplaceFilter;
import com.jhlabs.image.InvertFilter;
import com.jhlabs.image.RippleFilter;
import com.jhlabs.image.SphereFilter;
import com.jhlabs.image.WaterFilter;
import com.octo.captcha.component.image.backgroundgenerator.BackgroundGenerator;
import com.octo.captcha.component.image.backgroundgenerator.EllipseBackgroundGenerator;
import com.octo.captcha.component.image.backgroundgenerator.FunkyBackgroundGenerator;
import com.octo.captcha.component.image.backgroundgenerator.GradientBackgroundGenerator;
import com.octo.captcha.component.image.backgroundgenerator.MultipleShapeBackgroundGenerator;
import com.octo.captcha.component.image.deformation.ImageDeformation;
import com.octo.captcha.component.image.deformation.ImageDeformationByFilters;
import com.octo.captcha.component.image.fontgenerator.DeformedRandomFontGenerator;
import com.octo.captcha.component.image.fontgenerator.FontGenerator;
import com.octo.captcha.component.image.fontgenerator.RandomFontGenerator;
import com.octo.captcha.component.image.fontgenerator.TwistedAndShearedRandomFontGenerator;
import com.octo.captcha.component.image.fontgenerator.TwistedRandomFontGenerator;
import com.octo.captcha.component.image.textpaster.BaffleRandomTextPaster;
import com.octo.captcha.component.image.textpaster.DoubleRandomTextPaster;
import com.octo.captcha.component.image.textpaster.RandomTextPaster;
import com.octo.captcha.component.image.textpaster.TextPaster;
import com.octo.captcha.component.image.wordtoimage.ComposedWordToImage;
import com.octo.captcha.component.image.wordtoimage.DeformedComposedWordToImage;
import com.octo.captcha.component.image.wordtoimage.WordToImage;
import com.octo.captcha.component.wordgenerator.RandomWordGenerator;
import com.octo.captcha.component.wordgenerator.WordGenerator;
import com.octo.captcha.engine.image.ListImageCaptchaEngine;
import com.octo.captcha.engine.image.gimpy.DefaultGimpyEngine;
import com.octo.captcha.image.ImageCaptchaFactory;
import com.octo.captcha.image.fisheye.FishEyeFactory;
import com.octo.captcha.image.gimpy.GimpyFactory;

/**
 * @author James Yong
 * @version $Id: Captcha.java,v 1.13 2007/08/06 15:38:01 rafaelsteil Exp $
 */
public class Captcha extends ListImageCaptchaEngine
{
	private static final Logger logger = Logger.getLogger(Captcha.class);
	
	private static Captcha classInstance = new Captcha();
	private List backgroundGeneratorList;
	private List textPasterList;
	private List fontGeneratorList;

	private static String charsInUse;
	
	private void initializeChars()
	{
		if (SystemGlobals.getBoolValue(ConfigKeys.CAPTCHA_IGNORE_CASE)) {
			charsInUse = "123456789abcdefghijlmnopkrstuvxzyk@#%^";
		}
		else {
			charsInUse = "123456789ABCDEFGHJLKMNPRSTWXYZabcdefghijlmnopkrstuvxzyk@#%^";
		}
	}
	
	/**
	 * Gets the singleton
	 * 
	 * @return Instance of Captcha class
	 */
	public static Captcha getInstance()
	{
		return classInstance;
	}

	protected void buildInitialFactories()
	{
		this.initializeChars();
		
		this.backgroundGeneratorList = new ArrayList();
		this.textPasterList = new ArrayList();
		this.fontGeneratorList = new ArrayList();
		
		int width = SystemGlobals.getIntValue(ConfigKeys.CAPTCHA_WIDTH);
		int height = SystemGlobals.getIntValue(ConfigKeys.CAPTCHA_HEIGHT);
		int minWords = SystemGlobals.getIntValue(ConfigKeys.CAPTCHA_MIN_WORDS);
		int maxWords = SystemGlobals.getIntValue(ConfigKeys.CAPTCHA_MAX_WORDS);
		int minFontSize = SystemGlobals.getIntValue(ConfigKeys.CAPTCHA_MIN_FONT_SIZE);
		int maxFontSize = SystemGlobals.getIntValue(ConfigKeys.CAPTCHA_MAX_FONT_SIZE);
		int fontColorRed = Integer
				.parseInt((String) ObjectUtils.defaultIfNull(
						SystemGlobals.getValue(ConfigKeys.CAPTCHA_FONT_RGB_RED),
						"255"));
		int fontColorGreen = Integer.parseInt((String) ObjectUtils
				.defaultIfNull(SystemGlobals
						.getValue(ConfigKeys.CAPTCHA_FONT_RGB_GREEN), "0"));
		int fontColorBlue = Integer.parseInt((String) ObjectUtils
				.defaultIfNull(SystemGlobals
						.getValue(ConfigKeys.CAPTCHA_FONT_RGB_BLUE), "0"));

		this.backgroundGeneratorList.add(new MultipleShapeBackgroundGenerator(
				new Integer(width), new Integer(height)));

		this.textPasterList.add(new RandomTextPaster(
				new Integer(minWords), new Integer(maxWords), new Color(fontColorRed, fontColorGreen, fontColorBlue)));

		this.fontGeneratorList.add(new RandomFontGenerator(new Integer(minFontSize), new Integer(maxFontSize)));

		// Create a random word generator
		WordGenerator words = new RandomWordGenerator(charsInUse);

		for (Iterator fontIter = this.fontGeneratorList.iterator(); fontIter.hasNext();) {
			FontGenerator fontGeny = (FontGenerator) fontIter.next();

			for (Iterator backIter = this.backgroundGeneratorList.iterator(); backIter.hasNext();) {
				BackgroundGenerator bkgdGeny = (BackgroundGenerator) backIter.next();

				for (Iterator textIter = this.textPasterList.iterator(); textIter.hasNext();) {
					TextPaster textPaster = (TextPaster) textIter.next();

					WordToImage word2image = new DeformedComposedWordToImage(
							fontGeny, bkgdGeny, textPaster,
							new ImageDeformationByFilters(
									new ImageFilter[] { new CrystalizeFilter() }),
							new ImageDeformationByFilters(
									new ImageFilter[] {  }),
							new ImageDeformationByFilters(
									new ImageFilter[] {  }));
					
//					ImageFilter[] filters={new WaterFilter(),new SphereFilter(),new RippleFilter(),new DisplaceFilter(),new InvertFilter(),};
					// Creates a ImageCaptcha Factory
					ImageCaptchaFactory factory = new GimpyFactory(words, word2image);
					
					// Add a factory to the gimpy list (A Gimpy is a ImagCaptcha)
					addFactory(factory);
				}	
			}
		}
	}

	public void writeCaptchaImage()
	{
		BufferedImage image = SessionFacade.getUserSession().getCaptchaImage();
		
		if (image == null) {
			return;
		}

		OutputStream outputStream = null;
		
		try {
			outputStream = JForumExecutionContext.getResponse().getOutputStream();
			ImageIO.write(image, "jpg", outputStream);
		}
		catch (IOException ex) {
			logger.error(ex);
		}
		finally {
			if (outputStream != null) {
				try {
					outputStream.close();
				}
				catch (IOException ex) {}
			}
		}
	}
}
