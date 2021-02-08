package com.woocommerce.android.ui.products.variations.attributes

import android.os.Bundle
import androidx.lifecycle.ViewModel
import androidx.savedstate.SavedStateRegistryOwner
import com.woocommerce.android.di.ViewModelAssistedFactory
import com.woocommerce.android.ui.products.variations.attributes.AddAttributeViewModel.Factory
import com.woocommerce.android.viewmodel.ViewModelKey
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap

@Module
abstract class AddAttributeModule {
    @Module
    companion object {
        @JvmStatic
        @Provides
        fun provideDefaultArgs(): Bundle? {
            return null
        }
    }
    @Binds
    @IntoMap
    @ViewModelKey(AddAttributeViewModel::class)
    abstract fun bindFactory(factory: Factory): ViewModelAssistedFactory<out ViewModel>

    @Binds
    abstract fun bindSavedStateRegistryOwner(fragment: AddAttributeFragment): SavedStateRegistryOwner
}