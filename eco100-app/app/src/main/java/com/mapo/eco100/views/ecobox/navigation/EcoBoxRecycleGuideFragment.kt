package com.mapo.eco100.views.ecobox.navigation

import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.mapo.eco100.databinding.EcoboxFragmentRecycleGuideBinding
import com.mapo.eco100.entity.staticmodel.FAQ
import com.mapo.eco100.views.ecobox.*


class EcoBoxRecycleGuideFragment : Fragment() {

    private var _binding: EcoboxFragmentRecycleGuideBinding? = null
    private val binding get() = _binding!!
    private lateinit var faqRecyclerView: RecyclerView
    internal var textlength = 0
    private var faqList = mutableListOf<FAQ>()
    var faqSearchList = mutableListOf<FAQ>()
    var adapter = FaqRecyclerAdapter()

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?
    ): View? {

        _binding = EcoboxFragmentRecycleGuideBinding.inflate(inflater, container, false)
        binding.searchIc.setColorFilter(Color.WHITE)


        faqRecyclerView = binding.ecoboxFaqRecyclerview
        faqRecyclerView.adapter = adapter
        faqRecyclerView.layoutManager = LinearLayoutManager(context)


        binding.searchEdit.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(edit: Editable?) {}
            override fun beforeTextChanged(charSequence: CharSequence?, start: Int, count: Int, after: Int) {}

            @RequiresApi(Build.VERSION_CODES.N)
            override fun onTextChanged(charSequence: CharSequence?, start: Int, before: Int, count: Int) {

                faqSearchList.clear()

                var strSequence = binding.searchEdit.text.toString()

                for(i in adapter.faqList.indices){
                    if(adapter.faqList[i].question.toString().contains(strSequence)||(adapter.faqList[i].answer.contains(strSequence)))
                        faqSearchList.add(adapter.faqList[i])
                    Log.d("faq","${adapter.faqList[i]}")
                }


//                adapter= FaqRecyclerAdapter()
//                faqRecyclerView.adapter=adapter
//                faqRecyclerView.layoutManager = LinearLayoutManager(context)
                adapter.onDataChanged(faqSearchList)
//                adapter.notifyDataSetChanged()

            }

        })


        binding.btnPaper.setOnClickListener {
            activity?.let {
                val intent = Intent(context, RecycleGuidePaperActivity::class.java)
                startActivity(intent)
            }
        }

        binding.btnGlass.setOnClickListener {
            activity?.let {
                val intent = Intent(context, RecycleGuideGlassActivity::class.java)
                startActivity(intent)

            }
        }

        binding.btnPlastic.setOnClickListener {
            activity?.let {
                val intent = Intent(context, RecycleGuidePlasticActivity::class.java)
                startActivity(intent)
            }
        }

        binding.btnCan.setOnClickListener {
            activity?.let {
                val intent = Intent(context, RecycleGuideCanActivity::class.java)
                startActivity(intent)
            }
        }

        binding.btnVinyl.setOnClickListener {
            activity?.let {
                val intent = Intent(context, RecycleGuideVinylActivity::class.java)
                startActivity(intent)
            }
        }

        binding.btnStyrofoam.setOnClickListener {
            activity?.let {
                val intent = Intent(context, RecycleGuideStyrofoamActivity::class.java)
                startActivity(intent)
            }
        }

        return binding.root

    }

//    fun refreshAdapter() {
//        // 수정된 데이터를 다시 로드
//        adapter?.onDataChanged(faqSearchList)
//        adapter?.notifyDataSetChanged()
//    }

    companion object {
        fun newInstance(): EcoBoxRecycleGuideFragment {
            return EcoBoxRecycleGuideFragment()
        }
    }


}



