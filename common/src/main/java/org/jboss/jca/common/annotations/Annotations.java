/*
 * IronJacamar, a Java EE Connector Architecture implementation
 * Copyright 2012, Red Hat Inc, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
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

package org.jboss.jca.common.annotations;

import org.jboss.jca.common.CommonBundle;
import org.jboss.jca.common.CommonLogger;
import org.jboss.jca.common.api.metadata.common.TransactionSupportEnum;
import org.jboss.jca.common.api.metadata.ra.AdminObject;
import org.jboss.jca.common.api.metadata.ra.AuthenticationMechanism;
import org.jboss.jca.common.api.metadata.ra.ConfigProperty;
import org.jboss.jca.common.api.metadata.ra.ConnectionDefinition;
import org.jboss.jca.common.api.metadata.ra.Connector;
import org.jboss.jca.common.api.metadata.ra.Connector.Version;
import org.jboss.jca.common.api.metadata.ra.CredentialInterfaceEnum;
import org.jboss.jca.common.api.metadata.ra.Icon;
import org.jboss.jca.common.api.metadata.ra.InboundResourceAdapter;
import org.jboss.jca.common.api.metadata.ra.LicenseType;
import org.jboss.jca.common.api.metadata.ra.LocalizedXsdString;
import org.jboss.jca.common.api.metadata.ra.MessageListener;
import org.jboss.jca.common.api.metadata.ra.OutboundResourceAdapter;
import org.jboss.jca.common.api.metadata.ra.RequiredConfigProperty;
import org.jboss.jca.common.api.metadata.ra.ResourceAdapter1516;
import org.jboss.jca.common.api.metadata.ra.SecurityPermission;
import org.jboss.jca.common.api.metadata.ra.XsdString;
import org.jboss.jca.common.api.metadata.ra.ra16.Activationspec16;
import org.jboss.jca.common.api.metadata.ra.ra16.ConfigProperty16;
import org.jboss.jca.common.api.metadata.ra.ra16.Connector16;
import org.jboss.jca.common.api.validator.ValidateException;
import org.jboss.jca.common.metadata.ra.common.AdminObjectImpl;
import org.jboss.jca.common.metadata.ra.common.AuthenticationMechanismImpl;
import org.jboss.jca.common.metadata.ra.common.ConnectionDefinitionImpl;
import org.jboss.jca.common.metadata.ra.common.InboundResourceAdapterImpl;
import org.jboss.jca.common.metadata.ra.common.MessageAdapterImpl;
import org.jboss.jca.common.metadata.ra.common.MessageListenerImpl;
import org.jboss.jca.common.metadata.ra.common.OutboundResourceAdapterImpl;
import org.jboss.jca.common.metadata.ra.common.ResourceAdapter1516Impl;
import org.jboss.jca.common.metadata.ra.common.SecurityPermissionImpl;
import org.jboss.jca.common.metadata.ra.ra16.Activationspec16Impl;
import org.jboss.jca.common.metadata.ra.ra16.ConfigProperty16Impl;
import org.jboss.jca.common.metadata.ra.ra16.Connector16Impl;
import org.jboss.jca.common.spi.annotations.repository.Annotation;
import org.jboss.jca.common.spi.annotations.repository.AnnotationRepository;

import java.io.Externalizable;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.resource.spi.Activation;
import javax.resource.spi.AdministeredObject;
import javax.resource.spi.ConnectionDefinitions;
import javax.resource.spi.TransactionSupport;
import javax.resource.spi.work.WorkContext;

import org.jboss.logging.Logger;
import org.jboss.logging.Messages;

/**
 * The annotation processor for JCA 1.6
 * @author <a href="mailto:jesper.pedersen@jboss.org">Jesper Pedersen</a>
 * @author <a href="mailto:jeff.zhang@jboss.org">Jeff Zhang</a>
 */
public class Annotations
{
   private static CommonBundle bundle = Messages.getBundle(CommonBundle.class);
   private static CommonLogger log = Logger.getMessageLogger(CommonLogger.class, Annotations.class.getName());

   private static boolean trace = log.isTraceEnabled();

   private enum Metadatas
   {
      RA, ACTIVATION_SPEC, MANAGED_CONN_FACTORY, ADMIN_OBJECT, PLAIN;
   };

   /**
    * Constructor
    */
   public Annotations()
   {
   }

   /**
    * Scan for annotations in the URLs specified
    * @param connector The connector adapter metadata
    * @param annotationRepository annotationRepository to use
    * @param classLoader The class loader used to generate the repository
    * @return The updated metadata
    * @exception Exception Thrown if an error occurs
    */
   public Connector merge(Connector connector, AnnotationRepository annotationRepository, ClassLoader classLoader)
      throws Exception
   {
      // Process annotations
      if (connector == null || connector.getVersion() == Version.V_16)
      {

         boolean isMetadataComplete = false;
         if (connector != null && connector instanceof Connector16)
         {
            isMetadataComplete = ((Connector16) connector).isMetadataComplete();
         }

         if (connector == null || !isMetadataComplete)
         {
            if (connector == null)
            {
               Connector annotationsConnector = process(annotationRepository, null, classLoader);
               connector = annotationsConnector;
            }
            else
            {
               Connector annotationsConnector = process(annotationRepository,
                  ((ResourceAdapter1516) connector.getResourceadapter()).getResourceadapterClass(),
                  classLoader);
               connector = connector.merge(annotationsConnector);
            }
         }
      }

      return connector;
   }

   /**
    * Process annotations
    * @param annotationRepository The annotation repository
    * @param xmlResourceAdapterClass resource adpater class name as define in xml
    * @param classLoader The class loader used to generate the repository
    * @return The updated metadata
    * @exception Exception Thrown if an error occurs
    */
   public Connector process(AnnotationRepository annotationRepository, String xmlResourceAdapterClass,
                            ClassLoader classLoader)
      throws Exception
   {
      if (annotationRepository == null)
         throw new ValidateException(bundle.annotationRepositoryNull());
      /* Process
         -------
         javax.resource.spi.Activation
         javax.resource.spi.AdministeredObject
         javax.resource.spi.AuthenticationMechanism
         javax.resource.spi.ConfigProperty
         javax.resource.spi.ConnectionDefinition
         javax.resource.spi.ConnectionDefinitions
         javax.resource.spi.Connector
         javax.resource.spi.SecurityPermission
      */

      // @ConfigProperty
      Map<Metadatas, ArrayList<ConfigProperty16>> configPropertiesMap =
         processConfigProperty(annotationRepository, classLoader);

      // @ConnectionDefinitions
      ArrayList<ConnectionDefinition> connectionDefinitions = 
         processConnectionDefinitions(annotationRepository, classLoader,
            configPropertiesMap == null ? null : configPropertiesMap.get(Metadatas.MANAGED_CONN_FACTORY),
            configPropertiesMap == null ? null : configPropertiesMap.get(Metadatas.PLAIN));

      // @ConnectionDefinition (outside of @ConnectionDefinitions)
      if (connectionDefinitions == null)
      {
         connectionDefinitions = new ArrayList<ConnectionDefinition>(1);
      }
      ArrayList<ConnectionDefinition> definitions =
         processConnectionDefinition(annotationRepository, classLoader,
            configPropertiesMap == null ? null : configPropertiesMap.get(Metadatas.MANAGED_CONN_FACTORY),
            configPropertiesMap == null ? null : configPropertiesMap.get(Metadatas.PLAIN));

      if (definitions != null)
         connectionDefinitions.addAll(definitions);

      connectionDefinitions.trimToSize();

      // @Activation
      InboundResourceAdapter inboundRA = 
         processActivation(annotationRepository, classLoader,
            configPropertiesMap == null ? null : configPropertiesMap.get(Metadatas.ACTIVATION_SPEC),
            configPropertiesMap == null ? null : configPropertiesMap.get(Metadatas.PLAIN));

      // @AdministeredObject
      ArrayList<AdminObject> adminObjs =
         processAdministeredObject(annotationRepository, classLoader,
            configPropertiesMap == null ? null : configPropertiesMap.get(Metadatas.ADMIN_OBJECT),
            configPropertiesMap == null ? null : configPropertiesMap.get(Metadatas.PLAIN));

      // @Connector
      Connector conn = processConnector(annotationRepository, classLoader, xmlResourceAdapterClass,
            connectionDefinitions, configPropertiesMap == null ? null : configPropertiesMap.get(Metadatas.RA),
            configPropertiesMap == null ? null : configPropertiesMap.get(Metadatas.PLAIN),
            inboundRA, adminObjs);

      return conn;
   }

   /**
    * Process: @Connector
    * @param annotationRepository The annotation repository
    * @param classLoader The class loader
    * @param xmlResourceAdapterClass resource adpater class name as define in xml
    * @param connectionDefinitions
    * @param configProperties
    * @param plainConfigProperties
    * @param inboundResourceadapter
    * @param adminObjs
    * @return The updated metadata
    * @exception Exception Thrown if an error occurs
    */
   private Connector processConnector(AnnotationRepository annotationRepository, ClassLoader classLoader, 
                                      String xmlResourceAdapterClass,
                                      ArrayList<ConnectionDefinition> connectionDefinitions,
                                      ArrayList<ConfigProperty16> configProperties,
                                      ArrayList<ConfigProperty16> plainConfigProperties,
                                      InboundResourceAdapter inboundResourceadapter,
                                      ArrayList<AdminObject> adminObjs)
      throws Exception
   {
      Connector connector = null;
      Collection<Annotation> values = annotationRepository.getAnnotation(javax.resource.spi.Connector.class);
      if (values != null)
      {
         if (values.size() == 1)
         {
            Annotation annotation = values.iterator().next();
            String raClass = annotation.getClassName();
            javax.resource.spi.Connector connectorAnnotation = (javax.resource.spi.Connector)annotation.getAnnotation();

            if (trace)
               log.trace("Processing: " + connectorAnnotation + " for " + raClass);

            connector = attachConnector(raClass, classLoader, connectorAnnotation, connectionDefinitions,
                                        configProperties, plainConfigProperties, inboundResourceadapter, adminObjs);
         }
         else if (values.size() == 0)
         {
            // JBJCA-240
            if (xmlResourceAdapterClass == null || xmlResourceAdapterClass.equals(""))
            {
               log.noConnector();
               throw new ValidateException(bundle.noConnectorDefined());
            }
         }
         else
         {
            // JBJCA-240
            if (xmlResourceAdapterClass == null || xmlResourceAdapterClass.equals(""))
            {
               log.moreThanOneConnector();
               throw new ValidateException(bundle.moreThanOneConnectorDefined());
            }
         }
      }
      else
      {
         connector = attachConnector(null, classLoader, null, connectionDefinitions, null, null,
                                     inboundResourceadapter, adminObjs);
      }

      return connector;
   }

   /**
    * Attach @Connector
    * @param raClass The class name for the resource adapter
    * @param classLoader The class loader
    * @param conAnnotation The connector
    * @param connectionDefinitions connectionDefinitions
    * @param configProperties  configProperties
    * @param plainConfigProperties plainConfigProperties
    * @param inboundResourceadapter inboundResourceadapter
    * @param adminObjs
    * @return The updated metadata
    * @exception Exception Thrown if an error occurs
    */
   private Connector attachConnector(String raClass, ClassLoader classLoader,
                                     javax.resource.spi.Connector conAnnotation,
                                     ArrayList<ConnectionDefinition> connectionDefinitions,
                                     ArrayList<ConfigProperty16> configProperties,
                                     ArrayList<ConfigProperty16> plainConfigProperties,
                                     InboundResourceAdapter inboundResourceadapter,
                                     ArrayList<AdminObject> adminObjs)
      throws Exception
   {
      // Vendor name
      XsdString vendorName = null;
      if (conAnnotation != null)
         vendorName = new XsdString(conAnnotation.vendorName(), null);

      // Description
      ArrayList<LocalizedXsdString> descriptions = null;
      if (conAnnotation != null && conAnnotation.description() != null && conAnnotation.description().length != 0)
      {
         descriptions = new ArrayList<LocalizedXsdString>(conAnnotation.description().length);
         for (String descriptionAnnoptation : conAnnotation.description())
         {
            descriptions.add(new LocalizedXsdString(descriptionAnnoptation, null));
         }
      }

      // Display name
      ArrayList<LocalizedXsdString> displayNames = null;
      if (conAnnotation != null && conAnnotation.description() != null && conAnnotation.displayName().length != 0)
      {
         displayNames = new ArrayList<LocalizedXsdString>(conAnnotation.displayName().length);
         for (String displayNameAnnotation : conAnnotation.displayName())
         {
            displayNames.add(new LocalizedXsdString(displayNameAnnotation, null));
         }
      }

      // EIS type
      XsdString eisType = null;
      if (conAnnotation != null)
         eisType = new XsdString(conAnnotation.eisType(), null);

      // License description
      // License required
      ArrayList<LocalizedXsdString> licenseDescriptions = null;

      if (conAnnotation != null && conAnnotation.licenseDescription() != null &&
          conAnnotation.licenseDescription().length != 0)
      {
         licenseDescriptions = new ArrayList<LocalizedXsdString>(conAnnotation.licenseDescription().length);
         for (String licenseDescriptionAnnotation : conAnnotation.licenseDescription())
         {
            licenseDescriptions.add(new LocalizedXsdString(licenseDescriptionAnnotation, null));
         }
      }
      LicenseType license = null;
      if (conAnnotation != null)
         license = new LicenseType(licenseDescriptions, conAnnotation.licenseRequired(), null);

      // RequiredWorkContext
      ArrayList<String> requiredWorkContexts = null;
      Class<? extends WorkContext>[] requiredWorkContextAnnotations = null;

      if (conAnnotation != null)
         requiredWorkContextAnnotations = conAnnotation.requiredWorkContexts();

      if (requiredWorkContextAnnotations != null)
      {
         requiredWorkContexts = new ArrayList<String>(requiredWorkContextAnnotations.length);
         for (Class<? extends WorkContext> requiredWorkContext : requiredWorkContextAnnotations)
         {

            if (!requiredWorkContexts.contains(requiredWorkContext.getName()))
            {
               if (trace)
                  log.trace("RequiredWorkContext=" + requiredWorkContext.getName());

               requiredWorkContexts.add(requiredWorkContext.getName());
            }
         }
      }

      // Large icon
      // Small icon
      ArrayList<Icon> icons = null;
      if (conAnnotation != null && ((conAnnotation.smallIcon() != null && conAnnotation.smallIcon().length != 0) ||
                                    (conAnnotation.largeIcon() != null && conAnnotation.largeIcon().length != 0)))
      {
         icons = new ArrayList<Icon>(
                                     (conAnnotation.smallIcon() == null ? 0 : conAnnotation.smallIcon().length) +
                                        (conAnnotation.largeIcon() == null ? 0 : conAnnotation.largeIcon().length));
         for (String smallIconAnnotation : conAnnotation.smallIcon())
         {
            icons.add(new Icon(new XsdString(smallIconAnnotation, null), null, null));
         }
         for (String largeIconAnnotation : conAnnotation.largeIcon())
         {
            icons.add(new Icon(null, new XsdString(largeIconAnnotation, null),  null));
         }
      }

      // Transaction support
      TransactionSupport.TransactionSupportLevel transactionSupportAnnotation = null;

      if (conAnnotation != null)
         transactionSupportAnnotation = conAnnotation.transactionSupport();

      if (transactionSupportAnnotation == null)
         transactionSupportAnnotation = TransactionSupport.TransactionSupportLevel.NoTransaction;

      TransactionSupportEnum transactionSupport = TransactionSupportEnum.valueOf(transactionSupportAnnotation.name());

      // Reauthentication support
      boolean reauthenticationSupport = false;
      if (conAnnotation != null)
         reauthenticationSupport = conAnnotation.reauthenticationSupport();

      // AuthenticationMechanism
      ArrayList<AuthenticationMechanism> authenticationMechanisms = null;
      if (conAnnotation != null)
         authenticationMechanisms = processAuthenticationMechanism(conAnnotation.authMechanisms());

      OutboundResourceAdapter outboundResourceadapter = new OutboundResourceAdapterImpl(connectionDefinitions,
                                                                                        transactionSupport,
                                                                                        authenticationMechanisms,
                                                                                        reauthenticationSupport, null);

      // Security permission
      ArrayList<SecurityPermission> securityPermissions = null;
      if (conAnnotation != null)
         securityPermissions = processSecurityPermissions(conAnnotation.securityPermissions());

      ArrayList<ConfigProperty> validProperties = new ArrayList<ConfigProperty>();

      if (configProperties != null)
      {
         validProperties.addAll(configProperties);
      }
      if (plainConfigProperties != null && raClass != null)
      {
         Set<String> raClasses = getClasses(raClass, classLoader);

         for (ConfigProperty configProperty16 : plainConfigProperties)
         {
            if (raClasses.contains(((ConfigProperty16Impl) configProperty16).getAttachedClassName()))
            {
               if (trace)
                  log.tracef("Attaching: %s (%s)", configProperty16, raClass);
                  
               validProperties.add(configProperty16);
            }
         }
      }

      validProperties.trimToSize();

      ResourceAdapter1516Impl resourceAdapter = new ResourceAdapter1516Impl(raClass, validProperties,
                                                                            outboundResourceadapter,
                                                                            inboundResourceadapter, adminObjs,
                                                                            securityPermissions, null);

      XsdString resourceadapterVersion = new XsdString("1.6", null);
      return new Connector16Impl("", vendorName, eisType, resourceadapterVersion, license, resourceAdapter,
                                 requiredWorkContexts, false, descriptions, displayNames, icons, null);

   }

   private ArrayList<SecurityPermission> processSecurityPermissions(
         javax.resource.spi.SecurityPermission[] securityPermissionAnotations)
   {
      ArrayList<SecurityPermission> securityPermissions = null;
      if (securityPermissionAnotations != null)
      {
         if (securityPermissionAnotations.length != 0)
         {
            securityPermissions = new ArrayList<SecurityPermission>(securityPermissionAnotations.length);
            for (javax.resource.spi.SecurityPermission securityPermission : securityPermissionAnotations)
            {
               SecurityPermission spmd = new SecurityPermissionImpl(
                                                                    null,
                                                                    new XsdString(
                                                                                  securityPermission.permissionSpec(),
                                                                                  null), null);
               securityPermissions.add(spmd);
            }
            securityPermissions.trimToSize();
         }
      }
      return securityPermissions;
   }

   private ArrayList<AuthenticationMechanism> processAuthenticationMechanism(
         javax.resource.spi.AuthenticationMechanism[] authMechanismAnnotations)
   {
      ArrayList<AuthenticationMechanism> authenticationMechanisms = null;
      if (authMechanismAnnotations != null)
      {
         authenticationMechanisms = new ArrayList<AuthenticationMechanism>(authMechanismAnnotations.length);
         for (javax.resource.spi.AuthenticationMechanism authMechanismAnnotation : authMechanismAnnotations)
         {
            ArrayList<LocalizedXsdString> descriptions = null;
            if (authMechanismAnnotation.description() != null && authMechanismAnnotation.description().length != 0)
            {
               descriptions = new ArrayList<LocalizedXsdString>(authMechanismAnnotation.description().length);
               for (String descriptionAnnoptation : authMechanismAnnotation.description())
               {
                  descriptions.add(new LocalizedXsdString(descriptionAnnoptation, null));
               }
            }
            XsdString authenticationMechanismType = new XsdString(authMechanismAnnotation
                  .authMechanism(), null);

            authenticationMechanisms.add(new AuthenticationMechanismImpl(descriptions, authenticationMechanismType,
                                                                         CredentialInterfaceEnum
                                                                            .valueOf(authMechanismAnnotation
                                                                               .credentialInterface()
                                                                               .name()), null));
         }
      }
      return authenticationMechanisms;
   }

   /**
    * Process: @ConnectionDefinitions
    * @param annotationRepository The annotation repository
    * @param classLoader The class loader
    * @param configProperties Config properties
    * @param plainConfigProperties Plain config properties
    * @return The updated metadata
    * @exception Exception Thrown if an error occurs
    */
   private ArrayList<ConnectionDefinition> processConnectionDefinitions(AnnotationRepository annotationRepository,
      ClassLoader classLoader,
      ArrayList<? extends ConfigProperty> configProperties,
      ArrayList<? extends ConfigProperty> plainConfigProperties)
      throws Exception
   {
      Collection<Annotation> values = annotationRepository.getAnnotation(ConnectionDefinitions.class);
      if (values != null)
      {
         if (values.size() == 1)
         {
            Annotation annotation = values.iterator().next();
            ConnectionDefinitions connectionDefinitionsAnnotation = (ConnectionDefinitions) annotation
               .getAnnotation();

            if (trace)
               log.trace("Processing: " + connectionDefinitionsAnnotation);

            return attachConnectionDefinitions(connectionDefinitionsAnnotation, annotation.getClassName(),
                                               classLoader,
                                               configProperties, plainConfigProperties);
         }
         else
            throw new ValidateException(bundle.moreThanOneConnectionDefinitionsDefined());
      }
      return null;

   }

   /**
    * Attach @ConnectionDefinitions
    * @param cds The connection definitions
    * @param mcf The managed connection factory
    * @param classLoader The class loader
    * @param configProperty The config properties
    * @param plainConfigProperty The lain config properties
    * @return The updated metadata
    * @exception Exception Thrown if an error occurs
    */
   private ArrayList<ConnectionDefinition> attachConnectionDefinitions(ConnectionDefinitions cds, String mcf,
      ClassLoader classLoader,                                                                       
      ArrayList<? extends ConfigProperty> configProperty,
      ArrayList<? extends ConfigProperty> plainConfigProperty)
      throws Exception
   {
      ArrayList<ConnectionDefinition> connectionDefinitions = null;

      if (cds.value() != null)
      {
         connectionDefinitions = new ArrayList<ConnectionDefinition>(cds.value().length);
         for (javax.resource.spi.ConnectionDefinition cd : cds.value())
         {
            connectionDefinitions.add(attachConnectionDefinition(mcf, cd, classLoader,
                                                                 configProperty, plainConfigProperty));
         }
      }

      return connectionDefinitions;
   }

   /**
    * Process: @ConnectionDefinition
    * @param annotationRepository The annotation repository
    * @param classLoader The class loader
    * @param configProperty The config properties
    * @param plainConfigProperty The plain config properties
    * @return The updated metadata
    * @exception Exception Thrown if an error occurs
    */
   private ArrayList<ConnectionDefinition> processConnectionDefinition(AnnotationRepository annotationRepository,
      ClassLoader classLoader,
      ArrayList<? extends ConfigProperty> configProperty,
      ArrayList<? extends ConfigProperty> plainConfigProperty)
      throws Exception
   {
      ArrayList<ConnectionDefinition> connectionDefinitions = null;

      Collection<Annotation> values = annotationRepository
         .getAnnotation(javax.resource.spi.ConnectionDefinition.class);
      if (values != null)
      {
         connectionDefinitions = new ArrayList<ConnectionDefinition>(values.size());

         for (Annotation annotation : values)
         {
            ConnectionDefinition cd = attachConnectionDefinition(annotation, classLoader,
                                                                 configProperty, plainConfigProperty);

            if (trace)
               log.tracef("Adding connection definition: %s", cd);

            connectionDefinitions.add(cd);
         }
      }

      return connectionDefinitions;
   }

   /**
    * Attach @ConnectionDefinition
    * @param annotation The annotation
    * @param classLoader The class loader
    * @param configProperty The config properties
    * @param plainConfigProperty The plain config properties
    * @return The updated metadata
    * @exception Exception Thrown if an error occurs
    */
   private ConnectionDefinition attachConnectionDefinition(Annotation annotation,
      ClassLoader classLoader,
      ArrayList<? extends ConfigProperty> configProperty,
      ArrayList<? extends ConfigProperty> plainConfigProperty)
      throws Exception
   {
      javax.resource.spi.ConnectionDefinition cd =
         (javax.resource.spi.ConnectionDefinition) annotation.getAnnotation();

      if (trace)
         log.trace("Processing: " + annotation);

      return attachConnectionDefinition(annotation.getClassName(), cd, classLoader,
                                        configProperty, plainConfigProperty);
   }

   /**
    * Attach @ConnectionDefinition
    * @param mcf The managed connection factory
    * @param cd The connection definition
    * @param classLoader The class loader
    * @param configProperties The config properties
    * @param plainConfigProperties The plain config properties
    * @return The updated metadata
    * @exception Exception Thrown if an error occurs
    */
   private ConnectionDefinition attachConnectionDefinition(String mcf, javax.resource.spi.ConnectionDefinition cd,
                                                           ClassLoader classLoader,
                                                           ArrayList<? extends ConfigProperty> configProperties,
                                                           ArrayList<? extends ConfigProperty> plainConfigProperties)
      throws Exception
   {
      if (trace)
         log.trace("Processing: " + cd);

      ArrayList<ConfigProperty> validProperties = new ArrayList<ConfigProperty>();

      if (configProperties != null)
      {
         for (ConfigProperty configProperty16 : configProperties)

         {
            if (mcf.equals(((ConfigProperty16Impl) configProperty16).getAttachedClassName()))
            {
               if (trace)
                  log.tracef("Attaching: %s (%s)", configProperty16, mcf);
                  
               validProperties.add(configProperty16);
            }
         }
      }
      if (plainConfigProperties != null)
      {
         Set<String> mcfClasses = getClasses(mcf, classLoader);

         for (ConfigProperty configProperty16 : plainConfigProperties)
         {
            if (mcfClasses.contains(((ConfigProperty16Impl) configProperty16).getAttachedClassName()))
            {
               if (trace)
                  log.tracef("Attaching: %s (%s)", configProperty16, mcf);
                  
               validProperties.add(configProperty16);
            }
         }
      }

      validProperties.trimToSize();

      XsdString connectionfactoryInterface = new XsdString(cd.connectionFactory().getName(), null);
      XsdString managedconnectionfactoryClass = new XsdString(mcf, null);
      XsdString connectionImplClass = new XsdString(cd.connectionImpl().getName(), null);
      XsdString connectionfactoryImplClass = new XsdString(cd.connectionFactoryImpl().getName(), null);
      XsdString connectionInterface = new XsdString(cd.connection().getName(), null);
      return new ConnectionDefinitionImpl(managedconnectionfactoryClass, validProperties,
                                          connectionfactoryInterface,
                                          connectionfactoryImplClass, connectionInterface, connectionImplClass, null);
   }

   /**
    * Process: @ConfigProperty
    * @param annotationRepository The annotation repository
    * @param classLoader The class loader to use
    * @return The updated metadata
    * @exception Exception Thrown if an error occurs
    */
   private Map<Metadatas, ArrayList<ConfigProperty16>> processConfigProperty(AnnotationRepository annotationRepository,
                                                                             ClassLoader classLoader)
      throws Exception
   {
      Map<Metadatas, ArrayList<ConfigProperty16>> valueMap = null;
      Collection<Annotation> values = annotationRepository.getAnnotation(javax.resource.spi.ConfigProperty.class);
      if (values != null)
      {
         valueMap = new HashMap<Annotations.Metadatas, ArrayList<ConfigProperty16>>();
         for (Annotation annotation : values)
         {
            javax.resource.spi.ConfigProperty configPropertyAnnotation = (javax.resource.spi.ConfigProperty) annotation
                  .getAnnotation();

            if (trace)
               log.trace("Processing: " + configPropertyAnnotation);

            XsdString configPropertyValue = XsdString.NULL_XSDSTRING;
            XsdString configPropertyName = new XsdString(getConfigPropertyName(annotation), null);
            if (configPropertyAnnotation.defaultValue() != null &&
                !configPropertyAnnotation.defaultValue().equals(""))
               configPropertyValue = new XsdString(configPropertyAnnotation.defaultValue(), null);
            XsdString configPropertyType;
            if (!Object.class.equals(configPropertyAnnotation.type()))
            {
               configPropertyType = new XsdString(configPropertyAnnotation.type().getName(), null);
            }
            else
            {
               configPropertyType = new XsdString(getConfigPropertyType(annotation, classLoader), null);
            }

            Boolean configPropertySupportsDynamicUpdates = configPropertyAnnotation.supportsDynamicUpdates();
            Boolean configPropertyConfidential = configPropertyAnnotation.confidential();
            // Description
            ArrayList<LocalizedXsdString> descriptions = null;
            if (configPropertyAnnotation.description() != null && configPropertyAnnotation.description().length != 0)
            {
               descriptions = new ArrayList<LocalizedXsdString>(configPropertyAnnotation.description().length);
               for (String descriptionAnnoptation : configPropertyAnnotation.description())
               {
                  descriptions.add(new LocalizedXsdString(descriptionAnnoptation, null));
               }
            }

            Boolean configPropertyIgnore = configPropertyAnnotation.ignore();

            String attachedClassName = annotation.getClassName();
            Class attachedClass = Class.forName(attachedClassName, true, classLoader);

            if (hasInterface(attachedClass, "javax.resource.spi.ResourceAdapter"))
            {
               ConfigProperty16 cfgMeta = new ConfigProperty16Impl(descriptions, configPropertyName,
                                                                   configPropertyType,
                                                                   configPropertyValue, configPropertyIgnore,
                                                                   configPropertySupportsDynamicUpdates,
                                                                   configPropertyConfidential, null);
               if (valueMap.get(Metadatas.RA) == null)
               {
                  valueMap.put(Metadatas.RA, new ArrayList<ConfigProperty16>());
               }
               valueMap.get(Metadatas.RA).add(cfgMeta);
            }
            else
            {
               ConfigProperty16 cfgMeta = new ConfigProperty16Impl(descriptions, configPropertyName,
                                                                   configPropertyType,
                                                                   configPropertyValue, configPropertyIgnore,
                                                                   configPropertySupportsDynamicUpdates,
                                                                   configPropertyConfidential, null,
                                                                   attachedClassName);
               if (hasInterface(attachedClass, "javax.resource.spi.ManagedConnectionFactory"))
               {
                  if (valueMap.get(Metadatas.MANAGED_CONN_FACTORY) == null)
                  {
                     valueMap.put(Metadatas.MANAGED_CONN_FACTORY, new ArrayList<ConfigProperty16>());
                  }
                  valueMap.get(Metadatas.MANAGED_CONN_FACTORY).add(cfgMeta);
               }
               else if (hasInterface(attachedClass, "javax.resource.spi.ActivationSpec"))
               {
                  if (hasNotNull(annotationRepository, annotation))
                  {
                     ((ConfigProperty16Impl)cfgMeta).setMandatory(true);
                  }

                  if (valueMap.get(Metadatas.ACTIVATION_SPEC) == null)
                  {
                     valueMap.put(Metadatas.ACTIVATION_SPEC, new ArrayList<ConfigProperty16>());
                  }
                  valueMap.get(Metadatas.ACTIVATION_SPEC).add(cfgMeta);
               }
               else if (hasAnnotation(attachedClass, AdministeredObject.class, annotationRepository))
               {
                  if (valueMap.get(Metadatas.ADMIN_OBJECT) == null)
                  {
                     valueMap.put(Metadatas.ADMIN_OBJECT, new ArrayList<ConfigProperty16>());
                  }
                  valueMap.get(Metadatas.ADMIN_OBJECT).add(cfgMeta);
               }
               else
               {
                  if (hasNotNull(annotationRepository, annotation))
                  {
                     ((ConfigProperty16Impl)cfgMeta).setMandatory(true);
                  }

                  if (valueMap.get(Metadatas.PLAIN) == null)
                  {
                     valueMap.put(Metadatas.PLAIN, new ArrayList<ConfigProperty16>());
                  }
                  valueMap.get(Metadatas.PLAIN).add(cfgMeta);
               }
            }
         }
         if (valueMap.get(Metadatas.RA) != null)
            valueMap.get(Metadatas.RA).trimToSize();
         if (valueMap.get(Metadatas.MANAGED_CONN_FACTORY) != null)
            valueMap.get(Metadatas.MANAGED_CONN_FACTORY).trimToSize();
         if (valueMap.get(Metadatas.ACTIVATION_SPEC) != null)
            valueMap.get(Metadatas.ACTIVATION_SPEC).trimToSize();
         if (valueMap.get(Metadatas.ADMIN_OBJECT) != null)
            valueMap.get(Metadatas.ADMIN_OBJECT).trimToSize();
         if (valueMap.get(Metadatas.PLAIN) != null)
            valueMap.get(Metadatas.PLAIN).trimToSize();
         return valueMap;
      }

      return valueMap;
   }

   /**
    * hasInterface
    *
    * @param c
    * @param targetClassName
    * @return
    */
   private boolean hasInterface(Class c, String targetClassName)
   {
      for (Class face : c.getInterfaces())
      {
         if (face.getName().equals(targetClassName))
         {
            return true;
         }
         else
         {
            for (Class face2 : face.getInterfaces())
            {
               if (face2.getName().equals(targetClassName))
               {
                  return true;
               }
               else if (hasInterface(face2, targetClassName))
               {
                  return true;
               }
            }
         }
      }
      if (null != c.getSuperclass())
      {
         return hasInterface(c.getSuperclass(), targetClassName);
      }
      return false;
   }

   /**
    * hasAnnotation, if class c contains annotation targetClass
    *
    * @param c
    * @param targetClass
    * @param annotationRepository
    * @return
    */
   private boolean hasAnnotation(Class c, Class targetClass, AnnotationRepository annotationRepository)
   {
      Collection<Annotation> values = annotationRepository.getAnnotation(targetClass);
      if (values == null)
         return false;
      for (Annotation annotation : values)
      {
         if (annotation.getClassName() != null && annotation.getClassName().equals(c.getName()))
            return true;
      }
      return false;

   }

   /**
    * Process: @AdministeredObject
    * @param md The metadata
    * @param annotationRepository The annotation repository
    * @param classLoader the classloadedr used to load annotated class
    * @param configProperties The config properties
    * @param plainConfigProperties The plain config properties
    * @return The updated metadata
    * @exception Exception Thrown if an error occurs
    */
   private ArrayList<AdminObject> processAdministeredObject(AnnotationRepository annotationRepository,
      ClassLoader classLoader, ArrayList<ConfigProperty16> configProperties,
      ArrayList<ConfigProperty16> plainConfigProperties)
      throws Exception
   {
      ArrayList<AdminObject> adminObjs = null;
      Collection<Annotation> values = annotationRepository.getAnnotation(AdministeredObject.class);
      if (values != null)
      {
         adminObjs = new ArrayList<AdminObject>(values.size());
         for (Annotation annotation : values)
         {
            AdministeredObject a = (AdministeredObject) annotation.getAnnotation();

            if (trace)
               log.trace("Processing: " + a);
            String aoName = null;
            String aoClassName = annotation.getClassName();
            Class<?> aClass = Class.forName(aoClassName, true, classLoader);
            List<Class<?>> declaredInterfaces = null;
            if (aClass.getInterfaces() != null && aClass.getInterfaces().length != 0)
            {
               declaredInterfaces = Arrays.asList(aClass.getInterfaces());

            }
            else
            {
               declaredInterfaces = Collections.emptyList();
            }
            if (a.adminObjectInterfaces() != null && a.adminObjectInterfaces().length > 0)
            {
               for (Class<?> annotatedInterface : a.adminObjectInterfaces())
               {
                  if (declaredInterfaces.contains(annotatedInterface) &&
                      !annotatedInterface.equals(Serializable.class) &&
                      !annotatedInterface.equals(Externalizable.class))
                  {
                     aoName = annotatedInterface.getName();
                     break;
                  }
               }
            }

            ArrayList<ConfigProperty> validProperties = new ArrayList<ConfigProperty>();

            if (configProperties != null)
            {
               for (ConfigProperty configProperty16 : configProperties)
               {
                  if (aoClassName.equals(((ConfigProperty16Impl) configProperty16).getAttachedClassName()))
                  {
                     if (trace)
                        log.tracef("Attaching: %s (%s)", configProperty16, aoClassName);
                  
                     validProperties.add(configProperty16);
                  }
               }
            }
            if (plainConfigProperties != null)
            {
               Set<String> aoClasses = getClasses(aoClassName, classLoader);

               for (ConfigProperty configProperty16 : plainConfigProperties)
               {
                  if (aoClasses.contains(((ConfigProperty16Impl) configProperty16).getAttachedClassName()))
                  {
                     if (trace)
                        log.tracef("Attaching: %s (%s)", configProperty16, aoClassName);
                  
                     validProperties.add(configProperty16);
                  }
               }
            }

            validProperties.trimToSize();

            XsdString adminobjectInterface = new XsdString(aoName, null);
            XsdString adminobjectClass = new XsdString(aoClassName, null);

            adminObjs.add(new AdminObjectImpl(adminobjectInterface, adminobjectClass, validProperties, null));
         }
      }

      return adminObjs;
   }

   /**
    * Process: @Activation
    * @param annotationRepository The annotation repository
    * @param classLoader The class loader
    * @param configProperties The config properties
    * @param plainConfigProperties The plain config properties
    * @return The updated metadata
    * @exception Exception Thrown if an error occurs
    */
   private InboundResourceAdapter processActivation(AnnotationRepository annotationRepository, ClassLoader classLoader,
                                                    ArrayList<ConfigProperty16> configProperties,
                                                    ArrayList<ConfigProperty16> plainConfigProperties)
      throws Exception
   {
      ArrayList<MessageListener> listeners = new ArrayList<MessageListener>();
      Collection<Annotation> values = annotationRepository.getAnnotation(Activation.class);
      if (values != null)
      {
         for (Annotation annotation : values)
         {
            listeners.addAll(attachActivation(annotation, classLoader, configProperties, plainConfigProperties));
         }
         listeners.trimToSize();
      }

      return new InboundResourceAdapterImpl(new MessageAdapterImpl(listeners, null), null);
   }

   /**
    * Attach @Activation
    * @param annotation The activation annotation
    * @param classLoader The class loader
    * @param configProperties The config properties
    * @param plainConfigProperties The plain config properties
    * @return The updated metadata
    * @exception Exception Thrown if an error occurs
    */
   private ArrayList<MessageListener> attachActivation(Annotation annotation, ClassLoader classLoader,
                                                       ArrayList<ConfigProperty16> configProperties,
                                                       ArrayList<ConfigProperty16> plainConfigProperties)
      throws Exception
   {
      ArrayList<ConfigProperty> validProperties = new ArrayList<ConfigProperty>();
      ArrayList<RequiredConfigProperty> requiredConfigProperties = null;

      if (configProperties != null)
      {
         for (ConfigProperty configProperty16 : configProperties)
         {
            if (annotation.getClassName().equals(((ConfigProperty16Impl) configProperty16).getAttachedClassName()))
            {
               validProperties.add(configProperty16);

               if (configProperty16.isMandatory())
               {
                  if (requiredConfigProperties == null)
                     requiredConfigProperties = new ArrayList<RequiredConfigProperty>(1);

                  requiredConfigProperties.add(new RequiredConfigProperty(null,
                                                                          configProperty16.getConfigPropertyName(),
                                                                          null));
               }
            }
         }
      }
      if (plainConfigProperties != null)
      {
         Set<String> asClasses = getClasses(annotation.getClassName(), classLoader);
         for (ConfigProperty configProperty16 : plainConfigProperties)
         {
            if (asClasses.contains(((ConfigProperty16Impl) configProperty16).getAttachedClassName()))
            {
               validProperties.add(configProperty16);

               if (configProperty16.isMandatory())
               {
                  if (requiredConfigProperties == null)
                     requiredConfigProperties = new ArrayList<RequiredConfigProperty>(1);

                  requiredConfigProperties.add(new RequiredConfigProperty(null,
                                                                          configProperty16.getConfigPropertyName(),
                                                                          null));
               }
            }
         }
      }

      validProperties.trimToSize();

      Activation activation = (Activation) annotation.getAnnotation();
      ArrayList<MessageListener> messageListeners = null;
      if (trace)
         log.trace("Processing: " + activation);
      if (activation.messageListeners() != null)
      {
         messageListeners = new ArrayList<MessageListener>(activation.messageListeners().length);
         for (Class asClass : activation.messageListeners())
         {
            Activationspec16 asMeta = new Activationspec16Impl(new XsdString(annotation.getClassName(), null),
                                                               requiredConfigProperties,
                                                               validProperties,
                                                               null);
            MessageListener mlMeta = new MessageListenerImpl(new XsdString(asClass.getName(), null), asMeta, null);
            messageListeners.add(mlMeta);

         }
      }
      return messageListeners;
   }

   /**
    * Get the config-property-name for an annotation
    * @param annotation The annotation
    * @return The name
    * @exception ClassNotFoundException Thrown if a class cannot be found
    * @exception NoSuchFieldException Thrown if a field cannot be found
    * @exception NoSuchMethodException Thrown if a method cannot be found
    */
   private String getConfigPropertyName(Annotation annotation)
      throws ClassNotFoundException, NoSuchFieldException, NoSuchMethodException
   {
      if (annotation.isOnField())
      {
         return annotation.getMemberName();
      }
      else if (annotation.isOnMethod())
      {
         String name = annotation.getMemberName();

         if (name.startsWith("set"))
         {
            name = name.substring(3);
         }
         else if (name.startsWith("get"))
         {
            name = name.substring(3);
         }
         else if (name.startsWith("is"))
         {
            name = name.substring(2);
         }

         if (name.length() > 1)
         {
            return Character.toLowerCase(name.charAt(0)) + name.substring(1);
         }
         else
         {
            return Character.toString(Character.toLowerCase(name.charAt(0)));
         }
      }

      throw new IllegalArgumentException(bundle.unknownAnnotation(annotation));
   }

   /**
    * Get the config-property-type for an annotation
    * @param annotation The annotation
    * @param classLoader The class loader to use
    * @return The fully qualified classname
    * @exception ClassNotFoundException Thrown if a class cannot be found
    */
   @SuppressWarnings("unchecked")
   private String getConfigPropertyType(Annotation annotation,
                                        ClassLoader classLoader)
      throws ClassNotFoundException
   {
      if (annotation.isOnField())
      {
         Class clz = Class.forName(annotation.getClassName(), true, classLoader);

         while (!Object.class.equals(clz))
         {
            try
            {
               Field field = clz.getDeclaredField(annotation.getMemberName());

               return field.getType().getName();
            }
            catch (NoSuchFieldException nsfe)
            {
               clz = clz.getSuperclass();
            }
         }
      }
      else if (annotation.isOnMethod())
      {
         Class clz = Class.forName(annotation.getClassName(), true, classLoader);

         Class[] parameters = null;

         if (annotation.getParameterTypes() != null)
         {
            parameters = new Class[annotation.getParameterTypes().size()];

            for (int i = 0; i < annotation.getParameterTypes().size(); i++)
            {
               String parameter = annotation.getParameterTypes().get(i);
               parameters[i] = Class.forName(parameter, true, classLoader);
            }
         }

         while (!Object.class.equals(clz))
         {
            try
            {
               Method method = clz.getDeclaredMethod(annotation.getMemberName(), parameters);

               if (void.class.equals(method.getReturnType()))
               {
                  if (parameters != null && parameters.length > 0)
                  {
                     return parameters[0].getName();
                  }
               }
               else
               {
                  return method.getReturnType().getName();
               }
            }
            catch (NoSuchMethodException nsme)
            {
               clz = clz.getSuperclass();
            }
         }
      }

      throw new IllegalArgumentException(bundle.unknownAnnotation(annotation));
   }

   /**
    * Get the class names for a class and all of its super classes
    * @param name The name of the class
    * @param cl The class loader
    * @return The set of class names
    */
   private Set<String> getClasses(String name, ClassLoader cl)
   {
      Set<String> result = new HashSet<String>();
      
      try
      {
         Class<?> clz = Class.forName(name, true, cl);
         while (!Object.class.equals(clz))
         {
            result.add(clz.getName());
            clz = clz.getSuperclass();
         }
      }
      catch (Throwable t)
      {
         log.debugf("Couldn't load: %s", name);
      }

      return result;
   }

   /**
    * Has a NotNull annotation attached
    * @param annotationRepository The annotation repository
    * @param annotation The annotation being checked
    * @return True of the method/field contains the NotNull annotation; otherwise false
    */
   private boolean hasNotNull(AnnotationRepository annotationRepository, Annotation annotation)
   {
      Collection<Annotation> values = annotationRepository.getAnnotation(javax.validation.constraints.NotNull.class);

      if (values == null || values.size() == 0)
         return false;

      for (Annotation notNullAnnotation : values)
      {
         if (notNullAnnotation.getClassName().equals(annotation.getClassName()) &&
             notNullAnnotation.getMemberName().equals(annotation.getMemberName()))
            return true;
      }

      return false;
   }
}
