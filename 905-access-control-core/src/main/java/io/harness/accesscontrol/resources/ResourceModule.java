package io.harness.accesscontrol.resources;

import io.harness.accesscontrol.resources.resourcegroups.ResourceGroupService;
import io.harness.accesscontrol.resources.resourcegroups.ResourceGroupServiceImpl;
import io.harness.accesscontrol.resources.resourcegroups.persistence.ResourceGroupDao;
import io.harness.accesscontrol.resources.resourcegroups.persistence.ResourceGroupDaoImpl;
import io.harness.accesscontrol.resources.resourcegroups.persistence.ResourceGroupMorphiaRegistrar;
import io.harness.accesscontrol.resources.resourcetypes.ResourceTypeService;
import io.harness.accesscontrol.resources.resourcetypes.ResourceTypeServiceImpl;
import io.harness.accesscontrol.resources.resourcetypes.persistence.ResourceTypeDao;
import io.harness.accesscontrol.resources.resourcetypes.persistence.ResourceTypeDaoImpl;
import io.harness.accesscontrol.resources.resourcetypes.persistence.ResourceTypeMorphiaRegistrar;
import io.harness.morphia.MorphiaRegistrar;

import com.google.inject.AbstractModule;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.Multibinder;

public class ResourceModule extends AbstractModule {
  private static ResourceModule instance;

  public static synchronized ResourceModule getInstance() {
    if (instance == null) {
      instance = new ResourceModule();
    }
    return instance;
  }

  @Override
  protected void configure() {
    Multibinder<Class<? extends MorphiaRegistrar>> morphiaRegistrars =
        Multibinder.newSetBinder(binder(), new TypeLiteral<Class<? extends MorphiaRegistrar>>() {});

    morphiaRegistrars.addBinding().toInstance(ResourceTypeMorphiaRegistrar.class);
    bind(ResourceTypeDao.class).to(ResourceTypeDaoImpl.class);
    bind(ResourceTypeService.class).to(ResourceTypeServiceImpl.class);

    morphiaRegistrars.addBinding().toInstance(ResourceGroupMorphiaRegistrar.class);
    bind(ResourceGroupDao.class).to(ResourceGroupDaoImpl.class);
    bind(ResourceGroupService.class).to(ResourceGroupServiceImpl.class);

    registerRequiredBindings();
  }

  private void registerRequiredBindings() {}
}
