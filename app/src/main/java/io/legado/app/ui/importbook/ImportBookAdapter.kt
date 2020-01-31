package io.legado.app.ui.importbook

import android.content.Context
import androidx.documentfile.provider.DocumentFile
import io.legado.app.R
import io.legado.app.base.adapter.ItemViewHolder
import io.legado.app.base.adapter.SimpleRecyclerAdapter
import io.legado.app.constant.AppConst
import io.legado.app.utils.*
import kotlinx.android.synthetic.main.item_import_book.view.*
import org.jetbrains.anko.sdk27.listeners.onClick


class ImportBookAdapter(context: Context, val callBack: CallBack) :
    SimpleRecyclerAdapter<DocItem>(context, R.layout.item_import_book) {
    var selectedUris = linkedSetOf<String>()
    private var localUri = arrayListOf<String>()

    fun upBookHas(uriList: List<String>) {
        localUri.clear()
        localUri.addAll(uriList)
        notifyDataSetChanged()
    }

    override fun convert(holder: ItemViewHolder, item: DocItem, payloads: MutableList<Any>) {
        holder.itemView.apply {
            if (item.isDir) {
                iv_icon.setImageResource(R.drawable.ic_folder)
                iv_icon.visible()
                cb_select.invisible()
                ll_brief.gone()
            } else {
                if (localUri.contains(item.uri.toString())) {
                    iv_icon.setImageResource(R.drawable.ic_book_has)
                    iv_icon.visible()
                    cb_select.invisible()
                } else {
                    iv_icon.invisible()
                    cb_select.visible()
                }
                ll_brief.visible()
                tv_tag.text = item.name.substringAfterLast(".")
                tv_size.text = StringUtils.toSize(item.size)
                tv_date.text = AppConst.DATE_FORMAT.format(item.date)
            }
            tv_name.text = item.name
            cb_select.isChecked = selectedUris.contains(item.uri.toString())
            onClick {
                if (item.isDir) {
                    callBack.nextDoc(DocumentFile.fromSingleUri(context, item.uri)!!)
                } else {
                    cb_select.isChecked = !cb_select.isChecked
                    if (cb_select.isChecked) {
                        selectedUris.add(item.uri.toString())
                    } else {
                        selectedUris.remove(item.uri.toString())
                    }
                }
            }
        }
    }

    interface CallBack {
        fun nextDoc(doc: DocumentFile)
        fun upCountView()
    }

}