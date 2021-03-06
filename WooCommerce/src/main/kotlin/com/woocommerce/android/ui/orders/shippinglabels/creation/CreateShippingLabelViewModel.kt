package com.woocommerce.android.ui.orders.shippinglabels.creation

import android.os.Parcelable
import androidx.annotation.StringRes
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import com.woocommerce.android.R.string
import com.woocommerce.android.di.ViewModelAssistedFactory
import com.woocommerce.android.model.Address
import com.woocommerce.android.model.PaymentMethod
import com.woocommerce.android.model.ShippingLabelPackage
import com.woocommerce.android.model.ShippingRate
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.orders.details.OrderDetailRepository
import com.woocommerce.android.ui.orders.shippinglabels.ShippingLabelRepository
import com.woocommerce.android.ui.orders.shippinglabels.creation.CreateShippingLabelEvent.ShowAddressEditor
import com.woocommerce.android.ui.orders.shippinglabels.creation.CreateShippingLabelEvent.ShowPackageDetails
import com.woocommerce.android.ui.orders.shippinglabels.creation.CreateShippingLabelEvent.ShowPaymentDetails
import com.woocommerce.android.ui.orders.shippinglabels.creation.CreateShippingLabelEvent.ShowShippingRates
import com.woocommerce.android.ui.orders.shippinglabels.creation.CreateShippingLabelEvent.ShowSuggestedAddress
import com.woocommerce.android.ui.orders.shippinglabels.creation.CreateShippingLabelEvent.ShowWooDiscountBottomSheet
import com.woocommerce.android.ui.orders.shippinglabels.creation.CreateShippingLabelViewModel.UiState.Failed
import com.woocommerce.android.ui.orders.shippinglabels.creation.CreateShippingLabelViewModel.UiState.Loading
import com.woocommerce.android.ui.orders.shippinglabels.creation.CreateShippingLabelViewModel.UiState.WaitingForInput
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelAddressValidator.AddressType
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelAddressValidator.AddressType.DESTINATION
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelAddressValidator.AddressType.ORIGIN
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelAddressValidator.ValidationResult
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.Error
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.Error.AddressValidationError
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.Error.DataLoadingError
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.Error.PackagesLoadingError
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.Event
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.Event.AddressChangeSuggested
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.Event.AddressEditCanceled
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.Event.AddressInvalid
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.Event.AddressValidated
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.Event.AddressValidationFailed
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.Event.EditAddressRequested
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.Event.EditPackagingCanceled
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.Event.EditPaymentCanceled
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.Event.PackagesSelected
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.Event.PaymentSelected
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.Event.ShippingCarrierSelected
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.Event.ShippingCarrierSelectionCanceled
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.Event.SuggestedAddressAccepted
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.Event.SuggestedAddressDiscarded
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.SideEffect
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.State
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.StateMachineData
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.Step
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.Step.CarrierStep
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.Step.CustomsStep
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.Step.OriginAddressStep
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.Step.PackagingStep
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.Step.PaymentsStep
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.Step.ShippingAddressStep
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.StepStatus.DONE
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.StepStatus.NOT_READY
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.StepStatus.READY
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.StepsState
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.viewmodel.LiveDataDelegate
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.viewmodel.ResourceProvider
import com.woocommerce.android.viewmodel.SavedStateWithArgs
import com.woocommerce.android.viewmodel.ScopedViewModel
import kotlinx.android.parcel.Parcelize
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.WooCommerceStore
import java.math.BigDecimal
import java.text.DecimalFormat

class CreateShippingLabelViewModel @AssistedInject constructor(
    @Assisted savedState: SavedStateWithArgs,
    dispatchers: CoroutineDispatchers,
    private val orderDetailRepository: OrderDetailRepository,
    private val shippingLabelRepository: ShippingLabelRepository,
    private val stateMachine: ShippingLabelsStateMachine,
    private val addressValidator: ShippingLabelAddressValidator,
    private val site: SelectedSite,
    private val wooStore: WooCommerceStore,
    private val accountStore: AccountStore,
    private val resourceProvider: ResourceProvider
) : ScopedViewModel(savedState, dispatchers) {
    companion object {
        private const val STATE_KEY = "state"
    }

    private val arguments: CreateShippingLabelFragmentArgs by savedState.navArgs()

    val viewStateData = LiveDataDelegate(savedState, ViewState())
    private var viewState by viewStateData

    init {
        initializeStateMachine()
    }

    private fun initializeStateMachine() {
        val state = savedState.get<State>(STATE_KEY)
        if (state != null) {
            stateMachine.initialize(state)
        } else {
            stateMachine.start(arguments.orderIdentifier)
        }

        launch {
            stateMachine.transitions.collect { transition ->
                // save the current state
                savedState[STATE_KEY] = transition.state

                when (transition.state) {
                    is State.DataLoading -> {
                        viewState = viewState.copy(uiState = Loading)
                        handleResult { loadData(transition.state.orderId) }
                    }
                    is State.DataLoadingFailure -> viewState = viewState.copy(uiState = Failed)
                    is State.WaitingForInput -> {
                        viewState = viewState.copy(
                            uiState = WaitingForInput,
                            progressDialogState = ProgressDialogState(isShown = false)
                        )
                        updateViewState(transition.state.data)
                    }
                    is State.OriginAddressValidation -> {
                        handleResult(
                            progressDialogTitle = string.shipping_label_edit_address_validation_progress_title,
                            progressDialogMessage = string.shipping_label_edit_address_progress_message
                        ) {
                            validateAddress(transition.state.data.stepsState.originAddressStep.data, ORIGIN)
                        }
                    }
                    is State.ShippingAddressValidation -> {
                        handleResult(
                            progressDialogTitle = string.shipping_label_edit_address_validation_progress_title,
                            progressDialogMessage = string.shipping_label_edit_address_progress_message
                        ) {
                            validateAddress(transition.state.data.stepsState.shippingAddressStep.data, DESTINATION)
                        }
                    }
                    else -> {
                    }
                }
                transition.sideEffect?.let { sideEffect ->
                    when (sideEffect) {
                        SideEffect.NoOp -> {
                        }
                        is SideEffect.ShowError -> showError(sideEffect.error)
                        is SideEffect.OpenAddressEditor -> triggerEvent(
                            ShowAddressEditor(
                                sideEffect.address,
                                sideEffect.type,
                                sideEffect.validationResult
                            )
                        )
                        is SideEffect.ShowAddressSuggestion -> triggerEvent(
                            ShowSuggestedAddress(
                                sideEffect.entered,
                                sideEffect.suggested,
                                sideEffect.type
                            )
                        )
                        is SideEffect.ShowPackageOptions -> openPackagesDetails(sideEffect.shippingPackages)
                        is SideEffect.ShowCustomsForm -> handleResult { Event.CustomsFormFilledOut }
                        is SideEffect.ShowCarrierOptions -> openShippingCarrierRates(sideEffect.data)
                        is SideEffect.ShowPaymentOptions -> openPaymentDetails()
                    }
                }
            }
        }
    }

    private suspend fun handleResult(
        @StringRes progressDialogTitle: Int = 0,
        @StringRes progressDialogMessage: Int = 0,
        action: suspend () -> Event
    ) {
        if (progressDialogTitle != 0) {
            val progressDialogState = ProgressDialogState(
                isShown = true,
                title = progressDialogTitle,
                message = progressDialogMessage
            )
            viewState = viewState.copy(progressDialogState = progressDialogState)
        }
        stateMachine.handleEvent(action())
        if (progressDialogTitle != 0) {
            viewState = viewState.copy(progressDialogState = ProgressDialogState(isShown = false))
        }
    }

    private fun openShippingCarrierRates(data: StateMachineData) {
        triggerEvent(
            ShowShippingRates(
                data.remoteOrderId,
                data.stepsState.originAddressStep.data,
                data.stepsState.shippingAddressStep.data,
                data.stepsState.packagingStep.data
            )
        )
    }

    private fun openPackagesDetails(currentShippingPackages: List<ShippingLabelPackage>) {
        triggerEvent(
            ShowPackageDetails(
                orderIdentifier = arguments.orderIdentifier,
                shippingLabelPackages = currentShippingPackages
            )
        )
    }

    private fun openPaymentDetails() {
        triggerEvent(ShowPaymentDetails)
    }

    private fun updateViewState(stateMachineData: StateMachineData) {
        fun <T> Step<T>.mapToUiState(): StepUiState {
            if (!isVisible) return StepUiState.hide()

            val description = when (this) {
                is OriginAddressStep -> data.toString()
                is ShippingAddressStep -> data.toString()
                is PackagingStep -> data.stepDescription
                is CustomsStep -> null
                is CarrierStep -> null
                is PaymentsStep -> data.stepDescription
            }

            return when (status) {
                NOT_READY -> StepUiState.notDone(description)
                READY -> StepUiState.current(description)
                DONE -> StepUiState.done(description)
            }
        }

        fun StepsState.getOrderSummary(): OrderSummaryState {
            val isVisible = originAddressStep.status == DONE &&
                shippingAddressStep.status == DONE &&
                packagingStep.status == DONE &&
                (!customsStep.isVisible || customsStep.status == DONE) &&
                carrierStep.status == DONE
            if (!isVisible) return OrderSummaryState()

            return OrderSummaryState(
                isVisible = true, price = BigDecimal.TEN, discount = BigDecimal.ONE
            )
        }

        viewState = viewState.copy(
            originAddressStep = stateMachineData.stepsState.originAddressStep.mapToUiState(),
            shippingAddressStep = stateMachineData.stepsState.shippingAddressStep.mapToUiState(),
            packagingDetailsStep = stateMachineData.stepsState.packagingStep.mapToUiState(),
            customsStep = stateMachineData.stepsState.customsStep.mapToUiState(),
            carrierStep = stateMachineData.stepsState.carrierStep.mapToUiState(),
            paymentStep = stateMachineData.stepsState.paymentsStep.mapToUiState(),
            orderSummaryState = stateMachineData.stepsState.getOrderSummary()
        )
    }

    private fun showError(error: Error) {
        when (error) {
            DataLoadingError -> triggerEvent(ShowSnackbar(string.dashboard_stats_error))
            AddressValidationError -> triggerEvent(ShowSnackbar(string.dashboard_stats_error))
            PackagesLoadingError -> triggerEvent(ShowSnackbar(string.shipping_label_packages_loading_error))
        }
    }

    private suspend fun loadData(orderId: String): Event {
        val order = requireNotNull(orderDetailRepository.getOrder(orderId))
        val accountSettings = shippingLabelRepository.getAccountSettings().let {
            if (it.isError) return Event.DataLoadingFailed
            it.model!!
        }
        return Event.DataLoaded(
            remoteOrderId = order.remoteId,
            originAddress = getStoreAddress(),
            shippingAddress = order.shippingAddress,
            currentPaymentMethod = accountSettings.paymentMethods.find { it.id == accountSettings.selectedPaymentId }
        )
    }

    private fun getStoreAddress(): Address {
        val siteSettings = wooStore.getSiteSettings(site.get())
        return Address(
            company = site.get().name,
            firstName = accountStore.account.firstName,
            lastName = accountStore.account.lastName,
            phone = "",
            email = "",
            country = siteSettings?.countryCode ?: "",
            state = siteSettings?.stateCode ?: "",
            address1 = siteSettings?.address ?: "",
            address2 = siteSettings?.address2 ?: "",
            city = siteSettings?.city ?: "",
            postcode = siteSettings?.postalCode ?: ""
        )
    }

    private suspend fun validateAddress(address: Address, type: AddressType): Event {
        return when (val result = addressValidator.validateAddress(address, type)) {
            ValidationResult.Valid -> AddressValidated(address)
            is ValidationResult.SuggestedChanges -> AddressChangeSuggested(result.suggested)
            is ValidationResult.NotFound,
            is ValidationResult.Invalid,
            is ValidationResult.NameMissing -> AddressInvalid(address, result)
            is ValidationResult.Error -> AddressValidationFailed
        }
    }

    private val List<ShippingLabelPackage>.stepDescription: String?
        get() {
            if (isEmpty()) return null

            val firstLine = if (size == 1) {
                first().selectedPackage!!.title
            } else {
                // TODO properly test this during M3
                resourceProvider.getString(
                    string.shipping_label_multi_packages_items_count,
                    sumBy { it.items.size },
                    size
                )
            }

            val weightDimension = wooStore.getProductSettings(site.get())?.weightUnit ?: ""
            val stringResource = if (size == 1) {
                string.shipping_label_single_package_total_weight
            } else {
                string.shipping_label_multi_packages_total_weight
            }
            val weightFormatted = with(DecimalFormat()) {
                maximumFractionDigits = 4
                minimumFractionDigits = 0
                format(sumByDouble { it.weight })
            }

            val secondLine = resourceProvider.getString(
                stringResource,
                weightFormatted,
                weightDimension
            )

            return "$firstLine\n$secondLine"
        }

    private val PaymentMethod?.stepDescription: String?
        get() {
            if (this == null) return null
            return resourceProvider.getString(string.shipping_label_selected_payment_description, cardDigits)
        }

    fun retry() = stateMachine.handleEvent(Event.FlowStarted(arguments.orderIdentifier))

    fun onAddressEditConfirmed(address: Address) {
        stateMachine.handleEvent(AddressValidated(address))
    }

    fun onAddressEditCanceled() {
        stateMachine.handleEvent(AddressEditCanceled)
    }

    fun onSuggestedAddressDiscarded() {
        stateMachine.handleEvent(SuggestedAddressDiscarded)
    }

    fun onSuggestedAddressAccepted(address: Address) {
        stateMachine.handleEvent(SuggestedAddressAccepted(address))
    }

    fun onSuggestedAddressEditRequested(address: Address) {
        stateMachine.handleEvent(EditAddressRequested(address))
    }

    fun onPackagesUpdated(packages: List<ShippingLabelPackage>) {
        stateMachine.handleEvent(PackagesSelected(packages))
    }

    fun onPackagesEditCanceled() {
        stateMachine.handleEvent(EditPackagingCanceled)
    }

    fun onPaymentsUpdated(paymentMethod: PaymentMethod) {
        stateMachine.handleEvent(PaymentSelected(paymentMethod))
    }

    fun onPaymentsEditCanceled() {
        stateMachine.handleEvent(EditPaymentCanceled)
    }

    fun onShippingCarriersSelected(carriers: List<ShippingRate>) {
        stateMachine.handleEvent(ShippingCarrierSelected)
    }

    fun onShippingCarrierSelectionCanceled() {
        stateMachine.handleEvent(ShippingCarrierSelectionCanceled)
    }

    fun onWooDiscountInfoClicked() {
        triggerEvent(ShowWooDiscountBottomSheet)
    }

    fun onEditButtonTapped(step: FlowStep) {
        when (step) {
            FlowStep.ORIGIN_ADDRESS -> Event.EditOriginAddressRequested
            FlowStep.SHIPPING_ADDRESS -> Event.EditShippingAddressRequested
            FlowStep.PACKAGING -> Event.EditPackagingRequested
            FlowStep.CUSTOMS -> Event.EditCustomsRequested
            FlowStep.CARRIER -> Event.EditShippingCarrierRequested
            FlowStep.PAYMENT -> Event.EditPaymentRequested
        }.also { event ->
            event.let {
                stateMachine.handleEvent(it)
            }
        }
    }

    fun onContinueButtonTapped(step: FlowStep) {
        when (step) {
            FlowStep.ORIGIN_ADDRESS -> Event.OriginAddressValidationStarted
            FlowStep.SHIPPING_ADDRESS -> Event.ShippingAddressValidationStarted
            FlowStep.PACKAGING -> Event.PackageSelectionStarted
            FlowStep.CUSTOMS -> Event.CustomsDeclarationStarted
            FlowStep.CARRIER -> Event.ShippingCarrierSelectionStarted
            FlowStep.PAYMENT -> Event.PaymentSelectionStarted
        }.also { event ->
            event.let { stateMachine.handleEvent(it) }
        }
    }

    override fun onCleared() {
        super.onCleared()
        shippingLabelRepository.clearCache()
    }

    @Parcelize
    data class ViewState(
        val uiState: UiState = WaitingForInput,
        val originAddressStep: StepUiState? = null,
        val shippingAddressStep: StepUiState? = null,
        val packagingDetailsStep: StepUiState? = null,
        val customsStep: StepUiState? = null,
        val carrierStep: StepUiState? = null,
        val paymentStep: StepUiState? = null,
        val orderSummaryState: OrderSummaryState = OrderSummaryState(),
        val progressDialogState: ProgressDialogState = ProgressDialogState()
    ) : Parcelable

    enum class UiState {
        Loading, Failed, WaitingForInput
    }

    @Parcelize
    data class ProgressDialogState(
        val isShown: Boolean = false,
        @StringRes val title: Int = 0,
        @StringRes val message: Int = 0
    ) : Parcelable

    @Parcelize
    data class OrderSummaryState(
        val isVisible: Boolean = false,
        val price: BigDecimal = BigDecimal.ZERO,
        val discount: BigDecimal = BigDecimal.ZERO
    ) : Parcelable

    @Parcelize
    data class StepUiState(
        val isVisible: Boolean = true,
        val details: String? = null,
        val isEnabled: Boolean? = null,
        val isContinueButtonVisible: Boolean? = null,
        val isEditButtonVisible: Boolean? = null,
        val isHighlighted: Boolean? = null
    ) : Parcelable {
        companion object {
            fun hide() = StepUiState(
                isVisible = false
            )

            fun notDone(newDetails: String? = null) = StepUiState(
                details = newDetails,
                isEnabled = false,
                isContinueButtonVisible = false,
                isEditButtonVisible = false,
                isHighlighted = false
            )

            fun current(newDetails: String? = null) = StepUiState(
                details = newDetails,
                isEnabled = true,
                isContinueButtonVisible = true,
                isEditButtonVisible = false,
                isHighlighted = true
            )

            fun done(newDetails: String? = null) = StepUiState(
                details = newDetails,
                isEnabled = true,
                isContinueButtonVisible = false,
                isEditButtonVisible = true,
                isHighlighted = false
            )
        }
    }

    enum class FlowStep {
        ORIGIN_ADDRESS, SHIPPING_ADDRESS, PACKAGING, CUSTOMS, CARRIER, PAYMENT
    }

    @AssistedInject.Factory
    interface Factory : ViewModelAssistedFactory<CreateShippingLabelViewModel>
}
