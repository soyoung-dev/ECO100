package com.mapo.eco100.views.community.navigation

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.mapo.eco100.views.login.KakaoLoginUtils
import com.mapo.eco100.views.community.EnrollActivity
import com.mapo.eco100.R
import com.mapo.eco100.config.BOARD_CLICK
import com.mapo.eco100.config.BOARD_ENROLL
import com.mapo.eco100.databinding.FragmentBoardBinding
import com.mapo.eco100.views.community.ShowBoardActivity
import com.mapo.eco100.adapter.BoardAdapter
import com.mapo.eco100.entity.board.BoardReadForm
import com.mapo.eco100.entity.board.Boards
import com.mapo.eco100.service.BoardService
import com.mapo.eco100.viewmodel.BoardViewModel
import com.mapo.eco100.views.MainActivity
import com.mapo.eco100.config.NetworkSettings
import com.mapo.eco100.views.network.NoConnectedDialog
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class BoardFragment : Fragment() {

    private val viewModel: BoardViewModel by viewModels()

    private var _binding: FragmentBoardBinding? = null
    private val binding get() = _binding!!
    private lateinit var mainActivityContext: Context
    private lateinit var boardAdapter: BoardAdapter
    private var pageToLoad: Int = 0
    private var searchMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentBoardBinding.inflate(inflater, container, false)
        mainActivityContext = requireContext()


        boardAdapter = BoardAdapter(
            onClickItem = { board_data ->
                if (!NetworkSettings.isNetworkAvailable(mainActivityContext)) {
                    val dialog = NoConnectedDialog(mainActivityContext)
                    dialog.show()
                } else if (!KakaoLoginUtils(mainActivityContext).isLogin()) {
                    KakaoLoginUtils(mainActivityContext).login()
                } else {
                    val service: BoardService =
                        NetworkSettings.retrofit.build().create(BoardService::class.java)
                    val userId = mainActivityContext.getSharedPreferences("login",Context.MODE_PRIVATE)
                        .getLong("userId",-1)
                    service.read(board_data.boardId,userId).enqueue(object : Callback<BoardReadForm> {
                        override fun onResponse(
                            call: Call<BoardReadForm>,
                            response: Response<BoardReadForm>
                        ) {
                            if(response.isSuccessful) {
                                val intent = Intent(mainActivityContext, ShowBoardActivity::class.java)
                                intent.putExtra("board_data", response.body()!!)
                                startActivityForResult(intent, BOARD_CLICK)
                            }
                        }

                        override fun onFailure(call: Call<BoardReadForm>, t: Throwable) {

                            Toast.makeText(mainActivityContext, "글에 문제가 있습니다.", Toast.LENGTH_SHORT)
                                .show()
                        }
                    })
                }
            }
        )

        binding.searchBtn.setOnClickListener {
            if (!NetworkSettings.isNetworkAvailable(mainActivityContext)) {
                val dialog = NoConnectedDialog(mainActivityContext)
                dialog.show()
            } else {
                val word = binding.searchWord.text.toString()
                if(word != "") {
                    searchMode = true
                    viewModel.searchBoards(word.trim())
                }
            }
        }

        binding.swipeRefresh.setOnRefreshListener {
            if (!NetworkSettings.isNetworkAvailable(mainActivityContext)) {
                val dialog = NoConnectedDialog(mainActivityContext)
                dialog.show()
            } else {
                binding.swipeRefresh.isRefreshing = true
                if (searchMode) {
                    binding.searchWord.setText("")
                    viewModel.fetchBoards()
                    binding.swipeRefresh.isRefreshing = false
                    searchMode = false
                } else {
                    NetworkSettings.retrofit.build().create(BoardService::class.java)
                        .refreshBoards(0)
                        .enqueue(object : Callback<ArrayList<Boards>> {
                            override fun onResponse(
                                call: Call<ArrayList<Boards>>,
                                response: Response<ArrayList<Boards>>
                            ) {
                                boardAdapter.refreshBoards(response.body()!!)
                                (requireActivity() as MainActivity).runOnUiThread {
                                    binding.swipeRefresh.isRefreshing = false
                                    binding.sortMenuText.text = "최근 순"
                                }
                                searchModeRelease()
                                pageToLoad = 0
                            }

                            override fun onFailure(call: Call<ArrayList<Boards>>, t: Throwable) {

                            }
                        })
                }
            }
        }

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(
                mainActivityContext,
                LinearLayoutManager.VERTICAL, false
            )
            adapter = boardAdapter
        }

        binding.boardWriteButton.setOnClickListener {
            if (!NetworkSettings.isNetworkAvailable(mainActivityContext)) {
                val dialog = NoConnectedDialog(mainActivityContext)
                dialog.show()
            } else {
                startActivityForResult(
                    Intent(mainActivityContext, EnrollActivity::class.java),
                    BOARD_ENROLL
                )
            }
        }

        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.toolBar.inflateMenu(R.menu.sort_boards)
        binding.toolBar.overflowIcon = ContextCompat.getDrawable(
            mainActivityContext,
            R.drawable.ic_baseline_keyboard_arrow_down_24
        )

        if (!NetworkSettings.isNetworkAvailable(mainActivityContext)) {
            val dialog = NoConnectedDialog(mainActivityContext)
            dialog.show()
            return
        }

        viewModel.apply {
            boardsLiveData.observe(viewLifecycleOwner, {
                if(it.isEmpty()) {
                    Toast.makeText(mainActivityContext,"검색 결과가 없습니다.",Toast.LENGTH_SHORT).show()
                }
                boardAdapter.updateBoards(it)
            })

            loadingLiveData.observe(viewLifecycleOwner, { isLoading ->
                binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            })
        }

        binding.toolBar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.action_recent -> {
                    viewModel.fetchBoards()
                    binding.sortMenuText.text = "최근 순"
                    pageToLoad = 0
                    true
                }
                R.id.action_likes -> {
                    viewModel.fetchBoardsOrderByLikes()
                    binding.sortMenuText.text = "좋아요 순"
                    pageToLoad = 0
                    true
                }
                else -> false
            }
        }

        binding.recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val lastVisibleItemPosition =
                    (recyclerView.layoutManager as LinearLayoutManager)
                        .findLastCompletelyVisibleItemPosition()
                val itemTotalCount = boardAdapter.itemCount - 1
                if (!searchMode && (lastVisibleItemPosition == itemTotalCount)) {
                    if (!NetworkSettings.isNetworkAvailable(mainActivityContext)) {
                        val dialog = NoConnectedDialog(mainActivityContext)
                        dialog.show()
                    } else {
                        viewModel.loadingLiveData.value = true
                        if(binding.sortMenuText.text.toString() == "좋아요 순") {
                            NetworkSettings.retrofit.build().create(BoardService::class.java)
                                .addBoardsOrderByLikes(++pageToLoad)
                                .enqueue(object : Callback<ArrayList<Boards>> {
                                    override fun onResponse(
                                        call: Call<ArrayList<Boards>>,
                                        response: Response<ArrayList<Boards>>
                                    ) {
                                        boardAdapter.addBoards(response.body()!!)
                                        viewModel.loadingLiveData.value = false
                                    }

                                    override fun onFailure(
                                        call: Call<ArrayList<Boards>>,
                                        t: Throwable
                                    ) {

                                    }
                                })
                        } else {
                            NetworkSettings.retrofit.build().create(BoardService::class.java)
                                .refreshBoards(++pageToLoad)
                                .enqueue(object : Callback<ArrayList<Boards>> {
                                    override fun onResponse(
                                        call: Call<ArrayList<Boards>>,
                                        response: Response<ArrayList<Boards>>
                                    ) {
                                        boardAdapter.addBoards(response.body()!!)
                                        viewModel.loadingLiveData.value = false
                                    }

                                    override fun onFailure(
                                        call: Call<ArrayList<Boards>>,
                                        t: Throwable
                                    ) {

                                    }
                                })
                        }
                    }
                }
            }
        })
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode != Activity.RESULT_OK) {
            return
        }

        when (requestCode) {
            BOARD_ENROLL -> {
                Toast.makeText(mainActivityContext, "글 등록 성공", Toast.LENGTH_SHORT).show()
                viewModel.fetchBoards()
                boardAdapter.refreshBoards(viewModel.boardsLiveData.value!!)
                searchModeRelease()
            }
            BOARD_CLICK -> {

            }
        }
    }

    fun searchModeRelease() {
        searchMode = false
        binding.searchWord.text.clear()
    }

    companion object {
        fun newInstance(): BoardFragment {
            return BoardFragment()
        }
    }

}