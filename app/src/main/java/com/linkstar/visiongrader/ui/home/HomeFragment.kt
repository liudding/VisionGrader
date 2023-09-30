package com.linkstar.visiongrader.ui.home

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.linkstar.visiongrader.R
import com.linkstar.visiongrader.data.LoginRepository
import com.linkstar.visiongrader.databinding.FragmentHomeBinding
import com.linkstar.visiongrader.ui.home.HomeViewModel
import com.linkstar.visiongrader.ui.login.LoginViewModel
import com.linkstar.visiongrader.ui.login.LoginViewModelFactory

class HomeFragment : Fragment() {

    companion object {

    }

    private lateinit var viewModel: HomeViewModel

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val loginViewModel: LoginViewModel by activityViewModels<LoginViewModel>(factoryProducer = { LoginViewModelFactory() })

    private val loginRepository = LoginRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val navController = findNavController()

//        val currentBackStackEntry = navController.currentBackStackEntry!!
//        val savedStateHandle = currentBackStackEntry.savedStateHandle
//        savedStateHandle.getLiveData<Boolean>(LoginFragment.LOGIN_SUCCESSFUL)
//            .observe(currentBackStackEntry, Observer { success ->
//                if (success) {
//                    binding.username.text = "nihao"
//                }
//            })
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.username.text = loginRepository.user?.name ?: ""

        loginViewModel.loginResult.observe(viewLifecycleOwner, Observer {
            binding.username.text = it.success?.displayName
        })

        binding.buttonToScanner.setOnClickListener {
            findNavController().navigate(R.id.action_HomeFragment_to_ScannerFragment)
        }

        binding.username.text = "nihao"

//        GlobalScope.launch {
//            val user = UserDataStore.getUser()
//            if (user != null) {
//                binding.username.text = user.name
//            }
//        }

//        lifecycleScope.launch {
//            val user = UserDataStore.getUser()
//            if (user != null) {
//                binding.username.text = user.name
//            }
//        }


    }


    @Suppress("DEPRECATION")
    @Deprecated("Deprecated in Java")
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        Log.d("Home", "onActivityCreated")


//        loginViewModel.loginResult.observe(viewLifecycleOwner, Observer { result ->
//            result ?: return@Observer
//
//            result.success?.let {
//                Log.d("Home", "login success")
//                binding.username.text = result.success.displayName
//            }
//
//        })
    }




    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }



}