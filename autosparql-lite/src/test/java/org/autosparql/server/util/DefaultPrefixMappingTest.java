package org.autosparql.server.util;

import org.aksw.autosparql.server.util.DefaultPrefixMapping;
import org.junit.Test;

public class DefaultPrefixMappingTest
{

	@Test
	public void test()
	{
		System.out.println(DefaultPrefixMapping.INSTANCE.expandPrefix("rdf:type"));
		//assertTrue(DefaultPrefixMapping.INSTANCE.expandPrefix("rdf:type"));
	}

}
