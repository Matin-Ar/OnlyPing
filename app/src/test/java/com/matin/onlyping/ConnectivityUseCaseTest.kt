package com.matin.onlyping

import com.matin.onlyping.data.repository.NetworkRepository
import com.matin.onlyping.domain.usecase.ConnectivityUseCase
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations

class ConnectivityUseCaseTest {

    @Mock
    private lateinit var repository: NetworkRepository

    private lateinit var useCase: ConnectivityUseCase

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        useCase = ConnectivityUseCase(repository)
    }

    @Test
    fun `execute website check returns success when http succeeds`() = runBlocking {
        val host = "https://google.com"
        val port = "443"
        
        `when`(repository.resolveDns(host)).thenReturn("1.2.3.4")
        `when`(repository.checkTcp(host, 443)).thenReturn(100L)
        `when`(repository.checkHttp(host, 443)).thenReturn(200)

        val result = useCase.execute(host, port)

        assertTrue(result.isSuccess)
        assertEquals(200, result.status.http.ordinal) // Status.SUCCESS is 2
    }
}
