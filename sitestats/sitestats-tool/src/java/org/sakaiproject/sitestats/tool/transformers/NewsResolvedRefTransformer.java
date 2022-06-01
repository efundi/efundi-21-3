/**
 * Copyright (c) 2006-2018 The Apereo Foundation
 *
 * Licensed under the Educational Community License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *             http://opensource.org/licenses/ecl2
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.sakaiproject.sitestats.tool.transformers;

import java.util.Collections;
import java.util.List;
import org.sakaiproject.sitestats.api.event.detailed.EventDetail;
import org.sakaiproject.sitestats.api.event.detailed.news.FeedData;
import org.sakaiproject.util.ResourceLoader;

/**
 * View-layer logic for presenting the data contained in the ResolvedEventData object,
 * default mechanism of presentation is a simple K/V list
 * @author plukasew
 */
public class NewsResolvedRefTransformer
{
	/**
	 * Transforms FeedData for presentation to the user
	 * @param feed the data
	 * @param rl resource loader for i18n
	 * @return EventDetails for presentation
	 */
	public static List<EventDetail> transform(FeedData feed, ResourceLoader rl)
	{
		return Collections.singletonList(EventDetail.newText(rl.getString("de_news_feed"), feed.title));
	}
}
