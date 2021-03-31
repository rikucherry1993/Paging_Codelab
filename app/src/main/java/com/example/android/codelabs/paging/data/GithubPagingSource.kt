package com.example.android.codelabs.paging.data

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.example.android.codelabs.paging.api.GithubService
import com.example.android.codelabs.paging.api.IN_QUALIFIER
import com.example.android.codelabs.paging.model.Repo
import retrofit2.HttpException
import java.io.IOException

// GitHub page API is 1 based: https://developer.github.com/v3/#pagination
private const val GITHUB_STARTING_PAGE_INDEX = 1
private const val NETWORK_PAGE_SIZE = 50


class GithubPagingSource(private val service: GithubService,
                         private val query: String  ) : PagingSource<Int, Repo>() {

    /**
     * 调用时机：初次取得数据以及滑动取得更多数据时
     * 参数：LoadParams -- 保有（1）key:load页数的索引 （2）loadSize:每页的数据量
     * 返回：LoadResult -- （1）成功时：LoadResult.Page （2）失败时：LoadResult.Error
     *      LoadResult.Page的构造器又接收nextKey或者prevKey的值。刷到头了则为null。
     */
    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Repo> {

        val position = params.key ?: GITHUB_STARTING_PAGE_INDEX
        val apiQuery = query + IN_QUALIFIER

        return try {
            val response = service.searchRepos(apiQuery, position, params.loadSize)
            val repos = response.items
            val nextKey = if (repos.isEmpty()) {
                null
            } else {

                // initial load size = 3 * NETWORK_PAGE_SIZE
                // ensure we're not requesting duplicating items, at the 2nd request
                position + (params.loadSize / NETWORK_PAGE_SIZE)
            }
            LoadResult.Page(data = repos,
                    prevKey = if (position == GITHUB_STARTING_PAGE_INDEX) null else position - 1,
                    nextKey = nextKey)

        } catch (exception: IOException) {
            return LoadResult.Error(exception)
        } catch (exception: HttpException) {
            return LoadResult.Error(exception)
        }
    }

    /**
     * 调用时机：初次load结束后再次load新数据时、任何数据集发生变化时（比如滑动删除，数据库更新引起的失效，进程死亡等）
     *
     */
    override fun getRefreshKey(state: PagingState<Int, Repo>): Int? {
        return state.anchorPosition?.let {
            anchorPosition -> state.closestPageToPosition(anchorPosition)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(anchorPosition)?.nextKey?.minus(1)
        }
    }

}