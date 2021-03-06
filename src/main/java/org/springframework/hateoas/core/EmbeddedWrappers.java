/*
 * Copyright 2014-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.hateoas.core;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

import org.springframework.aop.support.AopUtils;
import org.springframework.hateoas.LinkRelation;
import org.springframework.hateoas.Resource;
import org.springframework.util.Assert;

/**
 * Interface to mark objects that are aware of the rel they'd like to be exposed under.
 *
 * @author Oliver Gierke
 */
public class EmbeddedWrappers {

	private final boolean preferCollections;

	/**
	 * Creates a new {@link EmbeddedWrappers}.
	 *
	 * @param preferCollections whether wrappers for single elements should rather treat the value as collection.
	 */
	public EmbeddedWrappers(boolean preferCollections) {
		this.preferCollections = preferCollections;
	}

	/**
	 * Creates a new {@link EmbeddedWrapper} that
	 *
	 * @param source
	 * @return
	 */
	public EmbeddedWrapper wrap(Object source) {
		return wrap(source, AbstractEmbeddedWrapper.NO_REL);
	}

	/**
	 * Creates an {@link EmbeddedWrapper} for an empty {@link Collection} with the given element type.
	 *
	 * @param type must not be {@literal null}.
	 * @return
	 */
	public EmbeddedWrapper emptyCollectionOf(Class<?> type) {
		return new EmptyCollectionEmbeddedWrapper(type);
	}

	/**
	 * Creates a new {@link EmbeddedWrapper} with the given rel.
	 *
	 * @param source can be {@literal null}, will return {@literal null} if so.
	 * @param rel must not be {@literal null} or empty.
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public EmbeddedWrapper wrap(Object source, LinkRelation rel) {

		if (source == null) {
			return null;
		}

		if (source instanceof EmbeddedWrapper) {
			return (EmbeddedWrapper) source;
		}

		if (source instanceof Collection) {
			return new EmbeddedCollection((Collection<Object>) source, rel);
		}

		if (preferCollections) {
			return new EmbeddedCollection(Collections.singleton(source), rel);
		}

		return new EmbeddedElement(source, rel);
	}

	private static abstract class AbstractEmbeddedWrapper implements EmbeddedWrapper {

		private static final LinkRelation NO_REL = LinkRelation.of("___norel___");

		private final LinkRelation rel;

		/**
		 * Creates a new {@link AbstractEmbeddedWrapper} with the given rel.
		 *
		 * @param rel must not be {@literal null} or empty.
		 */
		public AbstractEmbeddedWrapper(LinkRelation rel) {

			Assert.notNull(rel, "Rel must not be null or empty!");
			this.rel = rel;
		}

		/*
		 * (non-Javadoc)
		 * @see org.springframework.hateoas.hal.EmbeddedWrapper#getRel()
		 */
		@Override
		public Optional<LinkRelation> getRel() {

			return Optional.ofNullable(rel) //
					.filter(it -> !it.equals(NO_REL));
		}

		/*
		 * (non-Javadoc)
		 * @see org.springframework.hateoas.core.EmbeddedWrapper#hasRel(org.springframework.hateoas.LinkRelation)
		 */
		@Override
		public boolean hasRel(LinkRelation rel) {
			return this.rel.isSameAs(rel);
		}

		/*
		 * (non-Javadoc)
		 * @see org.springframework.hateoas.hal.EmbeddedWrapper#getRelTargetType()
		 */
		@Override
		@SuppressWarnings("unchecked")
		public Class<?> getRelTargetType() {

			Object peek = peek();

			if (peek == null) {
				return null;
			}

			peek = peek instanceof Resource ? ((Resource<Object>) peek).getContent() : peek;

			return AopUtils.getTargetClass(peek);
		}

		/**
		 * Peek into the wrapped element. The object returned is used to determine the actual value type of the wrapper.
		 *
		 * @return
		 */
		protected abstract Object peek();
	}

	/**
	 * {@link EmbeddedWrapper} for a single element.
	 *
	 * @author Oliver Gierke
	 */
	private static class EmbeddedElement extends AbstractEmbeddedWrapper {

		private final Object value;

		/**
		 * Creates a new {@link EmbeddedElement} for the given value and link relation.
		 *
		 * @param value must not be {@literal null}.
		 * @param relation must not be {@literal null}.
		 */
		public EmbeddedElement(Object value, LinkRelation relation) {

			super(relation);
			Assert.notNull(value, "Value must not be null!");
			this.value = value;
		}

		/*
		 * (non-Javadoc)
		 * @see org.springframework.hateoas.hal.EmbeddedWrapper#getValue()
		 */
		@Override
		public Object getValue() {
			return value;
		}

		/*
		 * (non-Javadoc)
		 * @see org.springframework.hateoas.EmbeddedWrappers.AbstractElementWrapper#peek()
		 */
		@Override
		protected Object peek() {
			return getValue();
		}

		/*
		 * (non-Javadoc)
		 * @see org.springframework.hateoas.hal.EmbeddedWrapper#isCollectionValue()
		 */
		@Override
		public boolean isCollectionValue() {
			return false;
		}
	}

	/**
	 * {@link EmbeddedWrapper} for a collection of elements.
	 *
	 * @author Oliver Gierke
	 */
	private static class EmbeddedCollection extends AbstractEmbeddedWrapper {

		private final Collection<Object> value;

		/**
		 * @param value must not be {@literal null} or empty.
		 * @param rel must not be {@literal null} or empty.
		 */
		public EmbeddedCollection(Collection<Object> value, LinkRelation rel) {

			super(rel);

			Assert.notNull(value, "Collection must not be null!");

			if (AbstractEmbeddedWrapper.NO_REL.equals(rel) && value.isEmpty()) {
				throw new IllegalArgumentException("Cannot wrap an empty collection with no rel given!");
			}

			this.value = value;
		}

		/*
		 * (non-Javadoc)
		 * @see org.springframework.hateoas.hal.EmbeddedWrapper#getValue()
		 */
		@Override
		public Collection<Object> getValue() {
			return value;
		}

		/*
		 * (non-Javadoc)
		 * @see org.springframework.hateoas.core.EmbeddedWrappers.AbstractEmbeddedWrapper#peek()
		 */
		@Override
		protected Object peek() {
			return value.isEmpty() ? null : value.iterator().next();
		}

		/*
		 * (non-Javadoc)
		 * @see org.springframework.hateoas.core.EmbeddedWrapper#isCollectionValue()
		 */
		@Override
		public boolean isCollectionValue() {
			return true;
		}
	}

	/**
	 * An {@link EmbeddedWrapper} to simulate a {@link Collection} of a given element type.
	 *
	 * @author Oliver Gierke
	 * @since 0.17
	 */
	private static class EmptyCollectionEmbeddedWrapper implements EmbeddedWrapper {

		private final Class<?> type;

		/**
		 * Creates a new {@link EmptyCollectionEmbeddedWrapper}.
		 *
		 * @param type must not be {@literal null}.
		 */
		public EmptyCollectionEmbeddedWrapper(Class<?> type) {

			Assert.notNull(type, "Element type must not be null!");
			this.type = type;
		}

		/*
		 * (non-Javadoc)
		 * @see org.springframework.hateoas.core.EmbeddedWrapper#getRel()
		 */
		@Override
		public Optional<LinkRelation> getRel() {
			return Optional.empty();
		}

		/*
		 * (non-Javadoc)
		 * @see org.springframework.hateoas.core.EmbeddedWrapper#getValue()
		 */
		@Override
		public Object getValue() {
			return Collections.emptySet();
		}

		/*
		 * (non-Javadoc)
		 * @see org.springframework.hateoas.core.EmbeddedWrapper#getRelTargetType()
		 */
		@Override
		public Class<?> getRelTargetType() {
			return type;
		}

		/*
		 * (non-Javadoc)
		 * @see org.springframework.hateoas.core.EmbeddedWrapper#isCollectionValue()
		 */
		@Override
		public boolean isCollectionValue() {
			return true;
		}

		/*
		 * (non-Javadoc)
		 * @see org.springframework.hateoas.core.EmbeddedWrapper#hasRel(org.springframework.hateoas.LinkRelation)
		 */
		@Override
		public boolean hasRel(LinkRelation rel) {
			return false;
		}
	}
}
